package services.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import services.RegisteredClient;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JobRegistrationRequest {
    private String payload;
    private Long startDate;
    private Long endDate;
    private Long interval;
    //private RegisteredClient client;
}
