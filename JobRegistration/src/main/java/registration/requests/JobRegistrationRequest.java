package registration.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JobRegistrationRequest {
    private String payload;
    private Long startDate;
    private Long endDate;
    private Long interval;
    private String callBackUrl;
    //private RegisteredClient client;


    public String getCallBackUrl() {
        return callBackUrl;
    }

    public Long getStartDate() {
        return startDate;
    }

    public Long getEndDate() {
        return endDate;
    }
}
