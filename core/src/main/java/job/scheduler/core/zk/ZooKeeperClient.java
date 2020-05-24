package job.scheduler.core.zk;

import com.google.common.util.concurrent.Uninterruptibles;
import job.scheduler.core.utils.TaskScheduler;
import job.scheduler.core.zk.exception.ZKClientErrorCode;
import job.scheduler.core.zk.exception.ZKClientException;
import job.scheduler.core.zk.response.ZkCreateNodeResponse;
import job.scheduler.core.zk.response.ZkExistsResponse;
import job.scheduler.core.zk.response.ZkGetDataResponse;
import job.scheduler.core.zk.response.ZkMultiResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.client.ZKClientConfig;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/***
 * ZookeeperClient class to handle all zookeeper connections.
 */
@Slf4j
public class ZooKeeperClient {
  // scheduler to handle zk connection expiry.
  private TaskScheduler expiryScheduler;
  private volatile ZooKeeper zooKeeper;

  private final String zkConnectionString;
  private final int zkSessionTimeOutMs;
  private final int zkConnectionTimeOutMs;

  // Locks
  private ReentrantReadWriteLock initializationLock = new ReentrantReadWriteLock();
  private ReentrantLock isConnectedOrExpiredLock = new ReentrantLock();
  private Condition isConnectedOrExpiredCondition = isConnectedOrExpiredLock.newCondition();

  // handler map
  final private ConcurrentHashMap<String, IZkStateChangeHandler> stateChangeHandlers;
  final private ConcurrentHashMap<String, IZkNodeChangeHandler> zkNodeChangeHandlers;

  private final ZKClientConfig zkClientConfig;

  public ZooKeeperClient(@NonNull final String zkConnectionString, final int zkSessionTimeOutMs,
                         final int zkConnectionTimeOutMs, ZKClientConfig zkClientConfig) throws InterruptedException, IOException {
    this.zkConnectionString = zkConnectionString;
    this.zkSessionTimeOutMs = zkSessionTimeOutMs;
    this.zkConnectionTimeOutMs = zkConnectionTimeOutMs;
    stateChangeHandlers = new ConcurrentHashMap<>();
    zkNodeChangeHandlers = new ConcurrentHashMap<>();
    this.zkClientConfig = zkClientConfig != null ? zkClientConfig : new ZKClientConfig();
    startup();
  }

  public ZkGetDataResponse getData(@NonNull String path) throws InterruptedException {
    boolean watch = zkNodeChangeHandlers.containsKey(path);
    log.debug("Watching {} path {} while getting data.", watch, path);
    Stat stat = new Stat();
    try {
      byte[] data = zooKeeper.getData(path, watch, stat);
      return new ZkGetDataResponse(KeeperException.Code.OK, path, data, stat);
    } catch (KeeperException ex) {
      log.warn("Error while getting data at path: " + path, ex);
      return new ZkGetDataResponse(ex.code(), path, null, stat);
    }
  }

  public ZkCreateNodeResponse create(@NonNull String path, byte[] data, List<ACL> acl,
                                     CreateMode createMode) throws InterruptedException {
    Stat stat = new Stat();
    try {
      String name = zooKeeper.create(path, data, acl, createMode, stat);
      return new ZkCreateNodeResponse(KeeperException.Code.OK, path, data, stat, name);
    } catch (KeeperException ex) {
      log.error("Error while creating path: " + path, ex);
      return new ZkCreateNodeResponse(ex.code(), path, data, stat, null);
    }
  }

  public ZkMultiResponse multi(@NonNull List<Op> ops) throws InterruptedException {
    try {
      List<OpResult> opResults = zooKeeper.multi(ops);
      return new ZkMultiResponse(KeeperException.Code.OK, null, opResults);
    } catch (KeeperException ex) {
      log.warn("Error while processing multi request ", ex);
      return new ZkMultiResponse(ex.code(), null, null);
    }
  }

  public ZkExistsResponse exits(@NonNull String path) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    boolean watch = zkNodeChangeHandlers.containsKey(path);
    log.debug("Watching {} path {} while checking exists.", watch, path);
    try {
      Stat stat = zooKeeper.exists(path, watch);
      return new ZkExistsResponse(KeeperException.Code.OK, path, stat);
    } catch (KeeperException ex) {
      log.warn("Error while checking exists path: " + path, ex);
      return new ZkExistsResponse(ex.code(), path, null);
    }
  }

  public void delete(@NonNull String path, int version) throws InterruptedException {
    try {
      zooKeeper.delete(path, version);
    } catch (KeeperException ex) {
      log.warn("Error while deleting path: " + path, ex);
    }
  }

  /*
   * Register handlers
   */

  public void registerStateChangeHandler(@NonNull IZkStateChangeHandler handler) {
    stateChangeHandlers.put(handler.name(), handler);
  }

  public void unRegisterStateHandler(@NonNull String name) {
    stateChangeHandlers.remove(name);
  }

  public void registerZkNodeHandler(@NonNull IZkNodeChangeHandler handler) {
    zkNodeChangeHandlers.put(handler.path(), handler);
  }

  public void unRegisterZkNodeHandler(@NonNull String path) {
    stateChangeHandlers.remove(path);
  }

  private void startup() throws InterruptedException, IOException {
    // starting new connection to zk.
    zooKeeper = new ZooKeeper(zkConnectionString, zkSessionTimeOutMs, new ZooKeeperClientWatcher(), zkClientConfig);
    expiryScheduler = new TaskScheduler(1, "zk-session-expiry-handler", false);
    expiryScheduler.startup();
    try {
      waitUntilConnected(zkConnectionTimeOutMs, TimeUnit.MILLISECONDS);
    } catch (Throwable ex) {
      shutdown();
      throw ex;
    }

  }

  private void waitUntilConnected(long time, TimeUnit unit) {
    log.info("Waiting for zookeeper to connect.");
    long nanos = unit.toNanos(time);
    ZooKeeper.States state = zooKeeper.getState();
    isConnectedOrExpiredLock.lock();
    try {
      while (!state.isConnected() && state.isAlive()) {
        if (nanos <= 0) {
          throw new ZKClientException("Timeout while wating for connection in state" + state.toString(), ZKClientErrorCode.CONNECTION_TIMEOUT);
        }
        try {
          nanos = isConnectedOrExpiredCondition.awaitNanos(nanos);
        } catch (InterruptedException ex) {
          log.warn("Awaking as zk client received event.");
        }

        state = zooKeeper.getState();
      }
      if (state == ZooKeeper.States.AUTH_FAILED) {
        throw new ZKClientException("Auth failed.", ZKClientErrorCode.AUTH_FAILED);
      }
      if (state == ZooKeeper.States.CLOSED) {
        throw new ZKClientException("Connection closed", ZKClientErrorCode.CONNECTION_CLOSED);
      }
    } finally {
      isConnectedOrExpiredLock.unlock();
    }
    log.info("Zookeeper connected.");
  }

  private void shutdown() throws InterruptedException {
    log.info("Shutting down zookeeper.");
    expiryScheduler.shutdown();
    initializationLock.writeLock().lock();
    try {
      zooKeeper.close();
    } finally {
      initializationLock.writeLock().unlock();
    }
    log.info("Shutdown zookeeper finished.");
  }

  private class ZooKeeperClientWatcher implements Watcher {

    @Override
    public void process(WatchedEvent event) {
      log.debug("Received event: {} and state: {}", event.getType().toString(), event.getState());
      if (event.getType() == Event.EventType.None) {

        Event.KeeperState state = event.getState();
        isConnectedOrExpiredLock.lock();
        try {
          isConnectedOrExpiredCondition.signalAll();
        } finally {
          isConnectedOrExpiredLock.unlock();
        }
        if (state == Event.KeeperState.AuthFailed) {
          log.error("Auth failed.");
          stateChangeHandlers.values().forEach(handler -> handler.onAuthFailure());
        } else if (state == Event.KeeperState.Expired) {
          scheduleSessionExpiryHandler();
        }
      } else {
        handleNodeChangeEvent(event);
      }
    }
  }

  private void handleNodeChangeEvent(WatchedEvent event) {
    final String path = event.getPath();
    if (StringUtils.isEmpty(path)) {
      log.info("Nothing to inform ", event);
      return;
    }
    log.info("Notified for {} path {}", event.getType().toString(), event.getPath());

    switch (event.getType()) {
      case NodeDataChanged: {
        zkNodeChangeHandlers.get(path).handleDataChange();
        break;
      }
      case NodeDeleted: {
        zkNodeChangeHandlers.get(path).handleDeletion();
        break;
      }
      case NodeCreated: {
        zkNodeChangeHandlers.get(path).handleCreation();
        break;
      }
      case NodeChildrenChanged: {
        zkNodeChangeHandlers.get(path).handleChildChange();
      }

    }
  }

  private void scheduleSessionExpiryHandler() {
    // schedule once
    expiryScheduler.schedule("zk-session-expired", new ReInitializeRunnable(), 0, -1, TimeUnit.MILLISECONDS);
  }

  private void reinitialize() throws InterruptedException {
    stateChangeHandlers.values().forEach(handler -> handler.beforeInitializingSession());
    initializationLock.writeLock().lock();
    try {
      ZooKeeper.States state = zooKeeper.getState();
      if (!state.isAlive()) {
        zooKeeper.close();
        log.info("Initializing a new session to {}.", zkConnectionString);
        // retry forever until ZooKeeper can be instantiated
        boolean connected = false;
        while (!connected) {
          try {
            zooKeeper = new ZooKeeper(zkConnectionString, zkSessionTimeOutMs, new ZooKeeperClientWatcher(), zkClientConfig);
            connected = true;
          } catch (Exception ex) {
            log.info("Error when recreating ZooKeeper, retrying after a short sleep", ex);
            Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);
          }
        }
      }
    } finally {
      initializationLock.writeLock().unlock();
    }

    stateChangeHandlers.values().forEach(handler -> handler.afterInitializingSession());

  }

  private class ReInitializeRunnable implements Runnable {
    @Override
    public void run() {
      try {
        reinitialize();
      } catch (InterruptedException ex) {
        log.info("Re initialization interrupted. ", ex);
      }
    }
  }
}
