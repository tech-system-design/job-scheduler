package job.scheduler.core;

import job.scheduler.core.config.ServerConfig;
import job.scheduler.core.server.JobSchedulerServer;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry class for Job scheduler.
 */
@Slf4j
public class JobScheduler {

  private static ServerConfig getConfigFromProps() {
    return new ServerConfig(String.valueOf(1), "localhost:2181", 18000);
  }

  public static void main(String[] args) {
    try {
      ServerConfig config = getConfigFromProps();
      JobSchedulerServer server = new JobSchedulerServer(config);
      server.startup();
      server.awaitShutdown();
    } catch (Throwable ex) {
      log.error("Exiting server due to fatal exception.");
      System.exit(1);
    }
    System.exit(0);
  }
}
