package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException.Code;

@ToString
public class ZkResponse {
  public Code resultCode;
  public String path;

  public ZkResponse(Code resultCode, String path) {
    this.resultCode = resultCode;
    this.path = path;
  }
}
