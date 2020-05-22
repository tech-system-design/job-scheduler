package job.scheduler.core.config;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
public class ServerConfig {
  @NonNull private String serverId;
  @NonNull private String zkConnectionString;
  @NonNull private int ZkTimeOutMs;
}
