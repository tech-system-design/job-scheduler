package job.scheduler.core.zk.response;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;


public class ZkGetDataResponse extends ZkResponse {
  public byte[] data;
  public Stat stat;

  public ZkGetDataResponse(KeeperException.Code resultCode, String path, Object ctx, byte[] data, Stat stat) {
    super(resultCode, path, ctx);
    this.data = data;
    this.stat = stat;
  }
}
