package job.scheduler.core.server;

import job.scheduler.core.config.ServerConfig;
import job.scheduler.core.controller.JobSchedulerController;
import job.scheduler.core.zk.ZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/***
 * Represent lifecycle of a job scheduler server.
 */
@Slf4j
public class JobSchedulerServer {
  private static final String CONTROLLER_PARENT = "/controller";
  private static final String SERVER_PARENT = "/servers";
  private AtomicBoolean startupComplete = new AtomicBoolean(false);
  private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
  private AtomicBoolean isStartingUp = new AtomicBoolean(false);
  private CountDownLatch shutdownLatch = new CountDownLatch(1);
  private ZookeeperClient zookeeperClient;
  private JobSchedulerController controller;
  private ServerConfig config;


  public JobSchedulerServer(ServerConfig config) {
    this.config = config;
  }

  public void startup() throws KeeperException, InterruptedException {

    try {
      log.info("Starting up server.");
      if (isShuttingDown.get()) {
        throw new IllegalStateException("Server is still shutting down. Can not restart.");
      }

      if (startupComplete.get()) {
        log.info("Already started up.");
        return;
      }

      boolean canStartup = isStartingUp.compareAndSet(false, true);

      if (canStartup) {
        initZkClient();
        startupController();

        shutdownLatch = new CountDownLatch(1);
        startupComplete.set(true);
        isStartingUp.set(false);
        log.info("Server started");
      }

    } catch (Throwable ex) {
      log.error("Fatal error during server startup. Preparing  for shutdown.", ex);
      isStartingUp.set(false);
      shutdown();
      System.exit(1);
    }
  }

  /**
   * Shutdown api to clean shutdown the server.
   */
  public void shutdown() {

    try {
      log.info("Shutting down server");
      if (isStartingUp.get()) {
        throw new IllegalStateException("Server is still starting up, cannot shut down!");
      }
      if (shutdownLatch.getCount() > 0
              && isShuttingDown.compareAndSet(false, true)) {
        controller.shutdown();

        startupComplete.set(false);
        isShuttingDown.set(false);
        shutdownLatch.countDown();
        log.info("Server shut down completed");
      }
    } catch (Throwable ex) {
      log.error("Fatal error during KafkaServer shutdown.", ex);
      isShuttingDown.set(false);
      Runtime.getRuntime().halt(1);
    }
  }

  /**
   * After calling shutdown this api waits until shutdown is completed.
   * @throws InterruptedException
   */
  public void awaitShutdown() throws InterruptedException {
    shutdownLatch.await();
  }

  private void initZkClient() throws KeeperException, InterruptedException, IOException {
    // TODO: init zk client first.
    log.info("Connecting to zookeeper on {}",  "");
    zookeeperClient = new ZookeeperClient(config.getZkConnectionString(), config.getZkTimeOutMs());
    zookeeperClient.tryCreateNode(CONTROLLER_PARENT, null, CreateMode.PERSISTENT);
    zookeeperClient.tryCreateNode(SERVER_PARENT, null, CreateMode.PERSISTENT);

  }

  private void startupController() throws KeeperException, InterruptedException {
    controller = new JobSchedulerController(zookeeperClient, config);
    controller.startup();
  }
}
