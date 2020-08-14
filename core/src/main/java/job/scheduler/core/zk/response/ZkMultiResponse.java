package job.scheduler.core.zk.response;

import lombok.ToString;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.OpResult;

import java.util.List;

@ToString
public class ZkMultiResponse extends ZkResponse {
  public List<OpResult> opResultList;

  public ZkMultiResponse(KeeperException.Code resultCode, String path, List<OpResult> opResultList) {
    super(resultCode, path);
    this.opResultList = opResultList;
  }
}
