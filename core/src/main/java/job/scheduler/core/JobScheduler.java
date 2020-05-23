package job.scheduler.core;

import job.scheduler.core.config.ServerConfig;
import job.scheduler.core.server.JobSchedulerServer;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Entry class for Job scheduler.
 */
@Slf4j
public class JobScheduler {

  private static ServerConfig getConfigFromProps(String[] args) {
    if (args == null || args.length != 1) {
      printMessageAndDie();
    }
    ServerConfig config = null;
    String propFile = args[0];
    try (InputStream input = new FileInputStream(propFile)) {

      Properties prop = new Properties();
      prop.load(input);
      config = new ServerConfig (
              Integer.parseInt(prop.getProperty(ServerConfig.SERVER_ID_KEY)),
              prop.getProperty(ServerConfig.ZK_CONNECTION_STRING_KEY),
              Integer.parseInt(prop.getProperty(ServerConfig.ZK_SESSION_TIME_OUT_KEY)),
              Integer.parseInt(prop.getProperty(ServerConfig.ZK_CONNECTION_TIME_OUT_KEY))
      );

    } catch (IOException ex) {
      log.error("Error while loading server.properties file.", ex);
      System.out.println("Failed to load server.properties");
      printMessageAndDie();
    }
    // TODO: read from props
    return config;
  }

  private static void printMessageAndDie() {
    String message = "USAGE: java [options] %s server.properties".format(JobScheduler.class.getSimpleName());
    System.out.println(message);
    System.exit(1);
  }

  public static void main(String[] args) {
    try {
      ServerConfig config = getConfigFromProps(args);
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
