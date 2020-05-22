package job.scheduler.core.zk;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

/***
 * ZookeeperClient class to handle all zookeeper connections.
 */
@Slf4j
public class ZookeeperClient {
  private LinkedHashSet<ZHandler> zHandlers;
  private ZooKeeper zkClient;

  public ZookeeperClient(@NonNull String zkConnectionString, int zkTimeoutMs) throws IOException {
    this.zHandlers = new LinkedHashSet<>();
    final ZKWatcher watcher = new ZKWatcher();
    zkClient = new ZooKeeper(zkConnectionString, zkTimeoutMs, watcher);
  }

  public interface ZHandler {
    void process(WatchedEvent watchedEvent) throws KeeperException, InterruptedException;

    void onZKSessionClosed();
  }

  @AllArgsConstructor
  private class ZKWatcher implements Watcher {

    @Override
    public void process(WatchedEvent event) {

      if (event.getType() == Event.EventType.None) {
        // We are are being told that the state of the
        // connection has changed
        switch (event.getState()) {
          case SyncConnected:
            // In this particular example we don't need to do anything
            // here - watches are automatically re-registered with
            // server and any watches triggered while the client was
            // disconnected will be delivered (in order of course)
            break;
          case Expired:
            // It's all over
            for (ZHandler handler :
                    zHandlers) {
              handler.onZKSessionClosed();
            }
            break;
        }
      } else {

        for (ZHandler handler :
                zHandlers) {
          try {
            handler.process(event);
          } catch (KeeperException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }


  public void registerZHandler(ZHandler zHandler) {
    if (zHandlers.contains(zHandler)) {
      log.info("Handler is already registered ", zHandler);
      return;
    }
    zHandlers.add(zHandler);
  }

  public void unRegisterZHandler(ZHandler zHandler) {
    if (zHandlers.contains(zHandler)) {
      zHandlers.remove(zHandler);
    }
  }

  public String tryCreateNode(@NonNull final String path, byte[] data, CreateMode mode) throws KeeperException, InterruptedException {
    if (zkClient.exists(path, false) == null) {
      final String created = zkClient.create(path, data /*data*/, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
      log.info("Path is created successfully {}.", path);
      return created;
    } else {
      log.info("Path is already created {}.", path);
      return path;
    }
  }

  public List<String> getChildren(@NonNull final String path) throws KeeperException, InterruptedException {
    return zkClient.getChildren(path, false);
  }
}
