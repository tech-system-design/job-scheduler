package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

@ToString
public class ZkExistsResponse extends ZkResponse {
  public Stat stat;
  public ZkExistsResponse(KeeperException.Code resultCode, String path, Stat stat) {
    super(resultCode, path);
    this.stat = stat;
  }
}
