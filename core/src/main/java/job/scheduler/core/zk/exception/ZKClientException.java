package job.scheduler.core.zk.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Zookeeper client exception.
 */
@AllArgsConstructor
public class ZKClientException extends RuntimeException {
  @Getter
  public String message;

  @Getter
  public ZKClientErrorCode code;
}
