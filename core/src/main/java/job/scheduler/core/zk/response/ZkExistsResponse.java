package job.scheduler.core.zk.response;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

public class ZkExistsResponse extends ZkResponse {
  public Stat stat;
  public ZkExistsResponse(KeeperException.Code resultCode, String path, Object ctx, Stat stat) {
    super(resultCode, path, ctx);
    this.stat = stat;
  }
}
