package job.scheduler.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor

public class ServerConfig {
  public final static String SERVER_ID_KEY = "serverId";
  public final static String ZK_CONNECTION_STRING_KEY = "zkConnectionString";
  public final static String ZK_SESSION_TIME_OUT_KEY = "zkSessionTimeOutMs";
  public final static String ZK_CONNECTION_TIME_OUT_KEY = "zkConnectionTimeOutMs";

  @Getter private int serverId;
  @Getter @NonNull private String zkConnectionString;
  @Getter private int zkSessionTimeOutMs;
  @Getter private int zkConnectionTimeOutMs;

}
