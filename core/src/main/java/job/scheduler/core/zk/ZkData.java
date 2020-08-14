package job.scheduler.core.zk;

import lombok.NonNull;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;

import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This file contains objects for encoding/decoding data stored in ZK (znodes)
 */
public class ZkData {
  public static class ControllerZNode {
    public static String path() {
      return "/controller";
    }
    public static int decode(byte[] data) {
      return Integer.parseInt(new String(data, UTF_8));
    }

    public static byte[] encode(int serverId) {
      return String.valueOf(serverId).getBytes(UTF_8);
    }
  }

  public static class ControllerEpochZNode {
    public static String  path() {
      return "/controller_epoch";
    }

    public static int decode(byte[] data) {
      return Integer.parseInt(new String(data, UTF_8));
    }

    public static byte[] encode(int epoch) {
      return String.valueOf(epoch).getBytes(UTF_8);
    }
  }

  public static class ServerZNode {
    public static String path() {
      return "/servers";
    }
  }

  public static class ServerIdsZNode {
    public static String path() {
      return ServerZNode.path() + "/ids";
    }
  }

  public static class ServerInfoZNode {
    public static String path(int id) {
      return ServerIdsZNode.path() + "/" + id;
    }
  }

  // TODO: return acls based on security
  public static List<ACL> defaultAcls() {
    return ZooDefs.Ids.OPEN_ACL_UNSAFE;
  }
}
