package job.scheduler.core.utils;


import com.google.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
/***
 * Scheduler class to run jobs.
 * A scheduler based on java.util.concurrent.ScheduledThreadPoolExecutor.
 */
public class TaskScheduler implements ITaskScheduler {

  private ScheduledThreadPoolExecutor executor = null;
  private AtomicInteger schedulerThreadId = new AtomicInteger(0);
  private int threads;
  private String threadNamePrefix;
  private boolean daemon;

  @Inject
  public TaskScheduler(int threads, @NonNull String threadNamePrefix, boolean daemon) {
    this.threads = threads;
    this.threadNamePrefix = threadNamePrefix;
    this.daemon = daemon;
  }

  @Override
  public void startup() {
    log.debug("Initializing task scheduler.");
    synchronized (this) {
      if (isStarted()) {
        throw new IllegalStateException("This scheduler has already been started!");
      }
      executor = new ScheduledThreadPoolExecutor(threads);
      executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
      executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      executor.setRemoveOnCancelPolicy(true);
      executor.setThreadFactory(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
          Thread thread = new Thread(runnable, threadNamePrefix + schedulerThreadId.getAndIncrement());
          thread.setDaemon(daemon);
          return thread;
        }
      });
    }
  }

  @Override
  public void shutdown() {
    log.debug("Shutting down task scheduler.");
    // We use the local variable to avoid NullPointerException if another thread shuts down scheduler at same time.
    ScheduledThreadPoolExecutor cachedExecutor = this.executor;
    if (cachedExecutor != null) {
      synchronized (this) {
        cachedExecutor.shutdown();
        this.executor = null;
      }
      try {
        cachedExecutor.awaitTermination(1, TimeUnit.DAYS);
      } catch (InterruptedException ex) {
        log.error("Error while shutting down.", ex);
      }
    }
  }

  @Override
  public boolean isStarted() {
    synchronized(this) {
      return executor != null;
    }
  }

  @Override
  public ScheduledFuture<?> schedule(String name, Runnable runnable, long delayMs, long periodMs, TimeUnit unit) {
    log.debug("Scheduling task %s with initial delay %d ms and period %d ms."
            .format(name, TimeUnit.MILLISECONDS.convert(delayMs, unit), TimeUnit.MILLISECONDS.convert(periodMs, unit)));
    synchronized (this) {
      ensureRunning();
      if (periodMs >= 0) {
        return executor.scheduleAtFixedRate(runnable, delayMs, periodMs, unit);
      } else {
        return executor.schedule(runnable, delayMs, unit);
      }
    }
  }

  private void ensureRunning() {
    if (!isStarted()) {
      throw new IllegalStateException("Kafka scheduler is not running.");
    }
  }
}
