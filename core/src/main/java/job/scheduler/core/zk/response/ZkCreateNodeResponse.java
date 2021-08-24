package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

@ToString
public class ZkCreateNodeResponse extends ZkGetDataResponse {
  public String name;

  public ZkCreateNodeResponse(KeeperException.Code resultCode, String path, byte[] data, Stat stat, String name) {
    super(resultCode, path, data, stat);
    this.name = name;
  }
}
