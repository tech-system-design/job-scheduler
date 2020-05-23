package services;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class JobSchedulerConfig extends Configuration {

    @JsonProperty("registeredClients")
    private RegisteredClient clients;

}
