package job.scheduler.core.zk;

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ZkDataTest {
  @Test
  public void testControllerZkNode() {
    final String expectedPath = "/controller";
    final int expectedControllerId = 1;
    Assert.assertEquals(expectedPath, ZkData.ControllerZNode.path());
    final byte[] encoded = ZkData.ControllerZNode.encode(expectedControllerId);
    Assert.assertEquals(expectedControllerId, ZkData.ControllerZNode.decode(encoded));
  }

  @Test
  public void testControllerEpochZkNode() {
    final String expectedPath = "/controller_epoch";
    final int expectedControllerEpoch = 10;
    Assert.assertEquals(expectedPath, ZkData.ControllerEpochZNode.path());
    final byte[] encoded = ZkData.ControllerEpochZNode.encode(expectedControllerEpoch);
    Assert.assertEquals(expectedControllerEpoch, ZkData.ControllerEpochZNode.decode(encoded));
  }

  @Test
  public void testServerZNode() {
    final String expectedPath = "/servers";
    Assert.assertEquals(expectedPath, ZkData.ServerZNode.path());
  }

  @Test
  public void testServerIdsZNode() {
    final String expectedPath = "/servers/ids";
    Assert.assertEquals(expectedPath, ZkData.ServerIdsZNode.path());
  }

  @Test
  public void testServerInfoZNode() {
    final int severId = 1;
    final String expectedPath = "/servers/ids/" + severId;
    Assert.assertEquals(expectedPath, ZkData.ServerInfoZNode.path(severId));
  }

  @Test
  public void testDefaultAcls() {
    List<ACL> expectedAcls = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    Assert.assertTrue(expectedAcls.equals(ZkData.defaultAcls()));
  }
}
