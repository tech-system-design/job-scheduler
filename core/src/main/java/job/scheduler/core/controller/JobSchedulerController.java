package job.scheduler.core.controller;

import job.scheduler.core.config.ServerConfig;
import job.scheduler.core.zk.ZookeeperClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.util.Collections;
import java.util.List;

/***
 * Main controller class responsible to assign work to other servers.
 */

@Slf4j
@RequiredArgsConstructor
public class JobSchedulerController {
  private static final String CONTROLLER_PARENT = "/controller";
  private static final String SERVER_PARENT = "/servers";
  private String electedNodeId;
  private boolean isLeader = false;
  private ControllerMonitor controllerMonitor;

  @NonNull private ZookeeperClient zkClient;
  @NonNull private ServerConfig config;

  /**
   * Invoked when job scheduler sever is startup. It does not assume any controller.
   * It register itself with zookeeper and start controller election.
   */
  public void startup() throws KeeperException, InterruptedException {
    registerServerWithZookeeper();
    electAndCache();
    startTaskDistribution();
  }

  /**
   * Invoked when job scheduler sever is shutting down.
   */
  public void shutdown() {

  }

  private boolean isLeader() {
    return isLeader;
  }

  private void startTaskDistribution() {
    if (!isLeader()) {
      // if current sever is not leader. quit
      return;
    }
    // TODO: start controller work in separate task

  }

  private void registerServerWithZookeeper() throws KeeperException, InterruptedException {
    electedNodeId = zkClient.tryCreateNode(CONTROLLER_PARENT + "/guid-", null, CreateMode.EPHEMERAL_SEQUENTIAL);
    zkClient.tryCreateNode(SERVER_PARENT + "/" + config.getServerId(), null, CreateMode.EPHEMERAL);
  }

  // TODO: make controller event management thread safe
  private void electAndCache() throws KeeperException, InterruptedException {
    isLeader = electedNodeId.equals(getLeaderNodePath());
    zkClient.unRegisterZHandler(controllerMonitor);
    String potentialLeader = getPotentialLeaderPathToWatch();
    controllerMonitor = new ControllerMonitor(potentialLeader);
    zkClient.registerZHandler(controllerMonitor);
  }

  private String getLeaderNodePath() throws KeeperException, InterruptedException {
    List<String> guids = zkClient.getChildren(CONTROLLER_PARENT);
    if (!guids.isEmpty()) {
      String leaderGUID = Collections.min(guids);
      log.info("leader is {}.", leaderGUID);
      return CONTROLLER_PARENT + "/" + leaderGUID;
    } else {
      // should not happen
      log.error("No server is registered.");
      return null;
    }
  }

  private String getPotentialLeaderPathToWatch() throws KeeperException, InterruptedException {
    if (isLeader()) {
      log.info("Current node {} is leader ", electedNodeId);
      return electedNodeId;
    }
    List<String> guids = zkClient.getChildren(CONTROLLER_PARENT);
    Collections.sort(guids);
    int index = Collections.binarySearch(guids, electedNodeId);
    if (index > 0) {
      return guids.get(index - 1);
    } else {
      return null;
    }
  }

  private class ControllerMonitor implements ZookeeperClient.ZHandler {
    private String potentialLeader;

    ControllerMonitor(String potentialLeader) {
      this.potentialLeader = potentialLeader;
    }

    @Override
    public void process(WatchedEvent watchedEvent) throws KeeperException, InterruptedException {
      String path = watchedEvent.getPath();
      if (StringUtils.isNotEmpty(path) && path.equals(potentialLeader)) {
        electAndCache();
      }
    }

    @Override
    public void onZKSessionClosed() {
      // reset connection
    }
  }

}
