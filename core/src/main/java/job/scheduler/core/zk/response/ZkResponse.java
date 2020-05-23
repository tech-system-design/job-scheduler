package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException.Code;

@ToString
public class ZkResponse {
  public Code resultCode;
  public String path;
  public Object ctx;

  public ZkResponse(Code resultCode, String path, Object ctx) {
    this.resultCode = resultCode;
    this.path = path;
    this.ctx = ctx;
  }
}