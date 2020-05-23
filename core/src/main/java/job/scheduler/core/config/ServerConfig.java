package job.scheduler.core.config;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
public class ServerConfig {
  @NonNull private int serverId;
  @NonNull private String zkConnectionString;
  @NonNull private int zkSessionTimeOutMs;
  @NonNull private int zkConnectionTimeOutMs;
}
