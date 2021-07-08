package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.ws.rs.core.MultivaluedMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobContext {
    private String jobId;
    private String callbackUrl;
    private MultivaluedMap payload;
    private Long nextScheduleEpoch;
}
