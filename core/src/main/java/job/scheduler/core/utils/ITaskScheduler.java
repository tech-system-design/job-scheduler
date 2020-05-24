package job.scheduler.core.utils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/***
 * A scheduler for running jobs.
 */
public interface ITaskScheduler {

  /**
   * Initialize this scheduler so it is ready to accept scheduling of tasks
   */
  void startup();

  /**
   * Shutdown this scheduler.
   * When this method is complete no more executions of background tasks will occur.
   * This includes tasks scheduled with a delayed execution.
   */
  void shutdown() throws InterruptedException;

  /**
   * Check if the scheduler has been started
   */
  boolean isStarted();

  /**
   * Schedule a task
   *
   * @param name   The name of this task
   * @param delay  The amount of time to wait before the first execution
   * @param period The period with which to execute the task. If < 0 the task will execute only once.
   * @param unit   The unit for the preceding times.
   * @return A Future object to manage the task scheduled.
   * @Param runnable The task to run
   */
  ScheduledFuture schedule(String name, Runnable runnable, long delay, long period, TimeUnit unit);
}
