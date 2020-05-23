package job.scheduler.core.zk;

import job.scheduler.core.utils.Pair;
import job.scheduler.core.zk.exception.ZKClientException;
import job.scheduler.core.zk.response.ZkCreateNodeResponse;
import job.scheduler.core.zk.response.ZkExistsResponse;
import job.scheduler.core.zk.response.ZkGetDataResponse;
import job.scheduler.core.zk.response.ZkMultiResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.client.ZKClientConfig;

import java.io.IOException;
import java.util.Arrays;

import static job.scheduler.core.controller.JobSchedulerController.INITIAL_CONTROLLER_EPOCH;
import static job.scheduler.core.controller.JobSchedulerController.INITIAL_CONTROLLER_EPOCH_ZK_VERSION;
import static job.scheduler.core.zk.exception.ZKClientErrorCode.CONTROLLER_MOVED;

@Slf4j
public class SchedulerZooKeeperClient extends ZooKeeperClient {
  public SchedulerZooKeeperClient(@NonNull String zkConnectionString, int zkSessionTimeOutMs,
                                  int zkConnectionTimeOutMs, ZKClientConfig zkClientConfig) throws InterruptedException, IOException {
    super(zkConnectionString, zkSessionTimeOutMs, zkConnectionTimeOutMs, zkClientConfig);
  }

  public int getActiveControllerIdOrElse(int fallback) throws InterruptedException, KeeperException {
    String path = ZkData.ControllerZNode.path();
    ZkGetDataResponse response = getData(path);
    switch (response.resultCode) {
      case OK:
        // return controller id
        return ZkData.ControllerZNode.decode(response.data);
      case NONODE:
        return fallback;
      default:
        //TODO: let the server fail. Might need better handling of such cases.
        throw KeeperException.create(response.resultCode, response.path);
    }
  }

  /**
   * Register server as controller in ZK and increment controller epoch.
   *
   * @param serverId server id of new controller
   * @return (Updated controller Epoch, epoch zkVersion)
   * @throws ZKClientException with CONTROLLER_MOVED error code if fail to create /controller or fail to increment controller epoch.
   */
  public Pair<Integer, Integer> registerControllerAndIncrementControllerEpoch(int serverId) throws ZKClientException, KeeperException, InterruptedException {
    // get controller epoch
    log.info("Registering controller and increasing controller epoch.");
    Pair<Integer, Integer> epoch = getControllerEpoch();
    if (epoch == null) {
      // may be create epoch.
      epoch = maybeCreateEpoch();
    }
    int newControllerEpoch = epoch.first + 1;
    int expectedControllerEpochZkVersion = epoch.second;


    Op createController = Op.create(ZkData.ControllerZNode.path(), ZkData.ControllerZNode.encode(serverId),
            ZkData.defaultAcls(), CreateMode.EPHEMERAL);
    Op setEpoch = Op.setData(ZkData.ControllerEpochZNode.path(),
            ZkData.ControllerEpochZNode.encode(newControllerEpoch), expectedControllerEpochZkVersion);
    ZkMultiResponse response = multi(Arrays.asList(createController, setEpoch));
    switch (response.resultCode) {
      case OK:
        OpResult.SetDataResult epochResponse = (OpResult.SetDataResult) response.opResultList.get(1);
        return new Pair<>(newControllerEpoch, epochResponse.getStat().getVersion());
      case BADVERSION:
      case NODEEXISTS:
        return checkControllerAndEpoch(serverId, newControllerEpoch);
    }
    throw new ZKClientException("Controller moved to another server." +
            " Aborting controller startup procedure", CONTROLLER_MOVED);
  }

  private Pair<Integer, Integer> checkControllerAndEpoch(int serverId, int newControllerEpoch) throws KeeperException, InterruptedException {
    int curControllerId = getActiveControllerIdOrElse(-1);
    if (curControllerId == -1) {
      throw new ZKClientException("Controller node went away hile checking controller election succeeds." +
              " Aborting controller startup.", CONTROLLER_MOVED);
    }
    if (serverId == curControllerId) {
      Pair<Integer, Integer> epoch = getControllerEpoch();
      if (epoch == null) {
        throw new IllegalStateException(
                ZkData.ControllerEpochZNode.path() + " existed before but goes away while trying to read it");
      }
      // If the epoch is the same as newControllerEpoch, it is safe to infer that the returned epoch zkVersion
      // is associated with the current broker during controller election because we already knew that the zk
      // transaction succeeds based on the controller znode verification. Other rounds of controller
      // election will result in larger epoch number written in zk.
      if (epoch.first == newControllerEpoch) {
        return new Pair<>(newControllerEpoch, epoch.second);
      }
    }
    throw new ZKClientException("Controller moved to another server." +
            " Aborting controller startup procedure", CONTROLLER_MOVED);
  }


  private Pair<Integer, Integer> getControllerEpoch() throws KeeperException, InterruptedException {
    log.info("Getting controller epoch.");
    String path = ZkData.ControllerEpochZNode.path();
    ZkGetDataResponse response = getData(path);
    log.info("Received controller epoch " +  response.resultCode);
    switch (response.resultCode) {
      case OK:
        // return epoch and zk version
        return new Pair<>(ZkData.ControllerEpochZNode.decode(response.data), response.stat.getVersion());
      case NONODE:
        return null;
      default:
        //TODO: let the server fail. Might need better handling of such cases.
        throw KeeperException.create(response.resultCode, response.path);
    }
  }

  private Pair<Integer, Integer> maybeCreateEpoch() throws InterruptedException, KeeperException, IllegalStateException {
    String path = ZkData.ControllerEpochZNode.path();
    log.info("Trying to create {}", path );
    ZkCreateNodeResponse response = create(path, ZkData.ControllerEpochZNode.encode(INITIAL_CONTROLLER_EPOCH),
            ZkData.defaultAcls(), CreateMode.PERSISTENT);
    log.info("Response {} while creating {}", response.resultCode, path);
    switch (response.resultCode) {
      case OK:
        // return epoch and zk version
        return new Pair<>(INITIAL_CONTROLLER_EPOCH, INITIAL_CONTROLLER_EPOCH_ZK_VERSION);
      case NODEEXISTS:
        Pair<Integer, Integer> controllerEpoch = getControllerEpoch();
        if (controllerEpoch == null) {
          throw new IllegalStateException(path + " Epoch existed before, goes away while reading");
        }
        return controllerEpoch;
      default:
        //TODO: let the server fail. Might need better handling of such cases.
        throw KeeperException.create(response.resultCode);
    }
  }

  public boolean registerZNodeChangeHandlerAndCheckExistence(@NonNull IZkNodeChangeHandler zkNodeChangeHandler)
          throws KeeperException, InterruptedException {
    registerZkNodeHandler(zkNodeChangeHandler);
    ZkExistsResponse existsResponse = exits(zkNodeChangeHandler.path());
    switch (existsResponse.resultCode) {
      case OK:
        return true;
      case NONODE:
        return false;
      default:
        throw KeeperException.create(existsResponse.resultCode);
    }
  }
}
