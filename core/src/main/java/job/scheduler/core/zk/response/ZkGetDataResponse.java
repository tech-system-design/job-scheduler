package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

@ToString
public class ZkGetDataResponse extends ZkResponse {
  public byte[] data;
  public Stat stat;

  public ZkGetDataResponse(KeeperException.Code resultCode, String path, byte[] data, Stat stat) {
    super(resultCode, path);
    this.data = data;
    this.stat = stat;
  }
}
