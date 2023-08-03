package tk.wangkexiong;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
public class AsyncJobExample {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    CompletableFuture<String> timeoutResult1 = CompletableFuture.supplyAsync(new TimeoutTask(5));
    CompletableFuture<String> timeoutResult2 = CompletableFuture.supplyAsync(new TimeoutTask(6));
    CompletableFuture<String> timeoutResult3 = CompletableFuture.supplyAsync(new TimeoutTask(7));
    CompletableFuture<String> timeoutResult4 = CompletableFuture.supplyAsync(new TimeoutTask(8));
    timeoutResult1 = CompletableFutureExpandUtils.orTimeout(timeoutResult1, 2, TimeUnit.SECONDS, new TimeoutHandler(5));
    timeoutResult2 = CompletableFutureExpandUtils.orTimeout(timeoutResult2, 4, TimeUnit.SECONDS, new TimeoutHandler(6));
    timeoutResult3 = CompletableFutureExpandUtils.orTimeout(timeoutResult3, 1, TimeUnit.SECONDS, new TimeoutHandler(7));
    timeoutResult4 =
      CompletableFutureExpandUtils
        .orTimeout(timeoutResult4, 2, TimeUnit.SECONDS)
        .handleAsync((r, ex) -> {
          log.info("Timeout for task {}", 8);
          return "Timeout";
        });

    log.info("task{}: {}", 5, timeoutResult1.get());
    log.info("task{}: {}", 6, timeoutResult2.get());
    log.info("task{}: {}", 7, timeoutResult3.get());
    log.info("task{}: {}", 8, timeoutResult4.get());
  }
}

@Slf4j
class TimeoutTask implements Supplier<String> {
  private final int taskID;

  TimeoutTask(int id) {
    this.taskID = id;
  }

  @Override
  public String get() {
    log.info("Start working for: " + taskID);
    try {
      Thread.sleep(3000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return "finished";
  }
}

@Slf4j
class TimeoutHandler implements Supplier<String> {
  private final int taskID;

  TimeoutHandler(int id) {
    this.taskID = id;
  }

  @Override
  public String get() {
    log.info("Timeout for task: " + taskID);

    return "Timeout";
  }
}