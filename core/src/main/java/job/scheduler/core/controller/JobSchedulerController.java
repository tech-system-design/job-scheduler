package job.scheduler.core.controller;

import job.scheduler.core.config.ServerConfig;
import job.scheduler.core.utils.Pair;
import job.scheduler.core.zk.*;
import job.scheduler.core.zk.exception.ZKClientErrorCode;
import job.scheduler.core.zk.exception.ZKClientException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;

/***
 * Main controller class responsible to assign work to other servers.
 */

@Slf4j
@RequiredArgsConstructor
public class JobSchedulerController {
  public static final int INITIAL_CONTROLLER_EPOCH = 0;
  public static final int INITIAL_CONTROLLER_EPOCH_ZK_VERSION = 0;
  public static final int INVALID_CONTROLLER_ID = -1;
  private int activeControllerId = INVALID_CONTROLLER_ID;

  private ControllerContext controllerContext;
  private ControllerChangeHandler controllerChangeHandler;

  @NonNull private SchedulerZooKeeperClient zkClient;
  @NonNull private ServerConfig config;

  /**
   * Invoked when job scheduler sever is startup. It does not assume any controller.
   * It register itself with zookeeper and start controller election.
   */
  public void startup() throws KeeperException, InterruptedException {
    controllerChangeHandler = new ControllerChangeHandler();
    controllerContext = new ControllerContext();
    zkClient.registerStateChangeHandler(new ControllerStateChangeHandler());
    processStartup();
    startTaskDistribution();
  }

  /**
   * Invoked when job scheduler sever is shutting down.
   */
  public void shutdown() {

  }

  private void processStartup() throws KeeperException, InterruptedException {
    log.info("Starting controller.");
    zkClient.registerZkNodeHandler(controllerChangeHandler);
    elect();
  }

  private void elect() throws KeeperException, InterruptedException {
    log.info("Electing {} as controller.", config.getServerId());
    // get current controller id from zookeeper
    activeControllerId = zkClient.getActiveControllerIdOrElse(INVALID_CONTROLLER_ID);

    if(activeControllerId != INVALID_CONTROLLER_ID) {
      log.info("Controller has been selected. Current server {} is controller. Stopping election process", activeControllerId);
      return;
    }

    try {
      Pair<Integer, Integer> epoch =  zkClient.registerControllerAndIncrementControllerEpoch(config.getServerId());
      activeControllerId = config.getServerId();
      controllerContext.epoch = epoch.first;
      controllerContext.epochZkVersion = epoch.second;
      log.info(activeControllerId + " has been successfully elected as controller.");
      log.info("Current epoch {} and version {} are ", epoch.first, epoch.second);
    } catch (ZKClientException ex) {
      if(ex.code == ZKClientErrorCode.CONTROLLER_MOVED) {
        // controller is moved to another server.
        maybeResign();
      } else {
        log.error("Error while electing controller. Retriggering controller election.");
        triggerControllerMove();
      }
    } catch (Throwable ex) {
      log.error("Error while electing controller. Retriggering controller election.");
      triggerControllerMove();
    }
  }

  private void maybeResign() throws KeeperException, InterruptedException {
    log.info("Checking if controller can be reassigned.");
    boolean wasActiveBeforeChange = isActive();
    //zkClient.registerZNodeChangeHandlerAndCheckExistence(controllerChangeHandler);
    activeControllerId = zkClient.getActiveControllerIdOrElse(INVALID_CONTROLLER_ID);
    log.info("Current server {}, wasactive: {}, isActive: {}", config.getServerId(), wasActiveBeforeChange, isActive());
    if (wasActiveBeforeChange && !isActive()) {
      onControllerResignation();
    }
  }

  private void triggerControllerMove() throws InterruptedException, KeeperException {
    activeControllerId = zkClient.getActiveControllerIdOrElse(INVALID_CONTROLLER_ID);
    if (!isActive()) {
     log.warn("Controller has already moved when trying to trigger controller movement");
      return;
    }
    try {
      int expectedControllerEpochZkVersion = controllerContext.epochZkVersion;
      activeControllerId = -1;
      onControllerResignation();
      zkClient.delete(ZkData.ControllerZNode.path(), expectedControllerEpochZkVersion);
    } catch (ZKClientException ex){
      if (ex.code == ZKClientErrorCode.CONTROLLER_MOVED) {
        log.warn("Controller has already moved when trying to trigger controller movement");
      }
    }
  }

  private void onControllerResignation() {
    log.debug("Resigning");
    // cleanup
    log.info("Resigned");
  }

  private boolean isActive() {
    return activeControllerId == config.getServerId();
  }

  private void startTaskDistribution() {
    if (!isActive()) {
      // if current sever is not leader. quit
      return;
    }
    // TODO: start controller work in separate task

  }

  private void registerServerAndReElect() {
    //_brokerEpoch = zkClient.registerBroker(brokerInfo)
    processReelect();
  }

  private void handleExpire() {
    activeControllerId = -1;
    onControllerResignation();
  }

  private void processControllerChange() {
    try {
      log.info("Processing controller change.");
      maybeResign();
    } catch (KeeperException | InterruptedException ex) {
      log.error("Failed to re-assign controller", ex);
    }
  }

  private void processReelect(){
    try {
      log.info("Process reelect");
      maybeResign();
      elect();
    } catch (KeeperException | InterruptedException ex) {
      log.error("Failed to re-elect controller", ex);
    }

  }

  private class ControllerChangeHandler implements IZkNodeChangeHandler {

    @Override
    public String path() {
      return ZkData.ControllerZNode.path();
    }

    @Override
    public void handleCreation() {
      processControllerChange();
    }

    @Override
    public void handleDeletion() {
      processReelect();
    }

    @Override
    public void handleDataChange() {
      processControllerChange();
    }

    @Override
    public void handleChildChange() {

    }
  }

  private class ControllerStateChangeHandler implements IZkStateChangeHandler {
    private static final String NAME = "controller-state-handler";
    @Override
    public String name() {
      return NAME;
    }

    @Override
    public void beforeInitializingSession() {
      handleExpire();
    }

    @Override
    public void afterInitializingSession() {
      registerServerAndReElect();
    }

    @Override
    public void onAuthFailure() {

    }
  }

}
