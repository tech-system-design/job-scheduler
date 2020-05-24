package services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import registration.RegisteredClient;

public class JobSchedulerConfig extends Configuration {

    @JsonProperty("registeredClients")
    private RegisteredClient clients;
    @JsonProperty("hotJobInterval")
    private Long hotJobInterval;
}
