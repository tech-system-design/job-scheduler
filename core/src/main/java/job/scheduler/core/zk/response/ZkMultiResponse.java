package job.scheduler.core.zk.response;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.OpResult;

import java.util.List;

public class ZkMultiResponse extends ZkResponse {
  public List<OpResult> opResultList;

  public ZkMultiResponse(KeeperException.Code resultCode, String path, Object ctx, List<OpResult> opResultList) {
    super(resultCode, path, ctx);
    this.opResultList = opResultList;
  }
}
