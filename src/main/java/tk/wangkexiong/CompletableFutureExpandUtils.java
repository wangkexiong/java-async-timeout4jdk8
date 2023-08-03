package tk.wangkexiong;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class CompletableFutureExpandUtils {
  static final class DelayScheduler {
    private final ScheduledThreadPoolExecutor executor;

    DelayScheduler() {
      this.executor = new ScheduledThreadPoolExecutor(
        1,
        r -> {
          Thread t = new Thread(r);
          t.setDaemon(true);
          t.setName("CompletableFutureExpandUtils-DelayScheduler");
          return t;
        });
      this.executor.setRemoveOnCancelPolicy(true);
    }

    public ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
      return executor.schedule(command, delay, unit);
    }
  }

  static final class TimeoutCommand implements Runnable {
    private final CompletableFuture<?> future;
    private final Supplier<?> timeoutHandler;

    TimeoutCommand(CompletableFuture<?> future, Supplier<?> timeoutHandler) {
      this.future = future;
      this.timeoutHandler = timeoutHandler;
    }

    @Override
    public void run() {
      if (null != future && !future.isDone()) {
        if (null != timeoutHandler) {
          future.complete(null);
          CompletableFuture.supplyAsync(timeoutHandler);
        } else {
          future.completeExceptionally(new TimeoutException());
        }
      }
    }
  }

  static final class RevocableTimeout implements BiConsumer<Object, Throwable> {
    private final ScheduledFuture<?> timeoutTask;

    RevocableTimeout(ScheduledFuture<?> timeoutTask) {
      this.timeoutTask = timeoutTask;
    }

    @Override
    public void accept(Object o, Throwable ex) {
      if (null == ex && null != timeoutTask && !timeoutTask.isDone()) {
        timeoutTask.cancel(false);
      }
    }
  }

  private final static DelayScheduler delayScheduler = new DelayScheduler();

  public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future,
                                                   long duration,
                                                   TimeUnit unit) {
    return orTimeout(future, duration, unit, null);
  }

  public static <T> CompletableFuture<T> orTimeout(CompletableFuture<T> future,
                                                   long duration,
                                                   TimeUnit unit,
                                                   Supplier<T> timeoutHandler) {
    if (null == future || null == unit) {
      return future;
    }

    if (future.isDone()) {
      return future;
    }

    ScheduledFuture<?> timeoutFuture =
      delayScheduler.delay(new TimeoutCommand(future, timeoutHandler), duration, unit);

    return future.whenCompleteAsync(new RevocableTimeout(timeoutFuture));
  }
}
