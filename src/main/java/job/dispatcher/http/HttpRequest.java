package job.dispatcher.http;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class HttpRequest {
    private Client client;

    @Inject
    public HttpRequest(){
         this.client = ClientBuilder.newClient();
    }

    public void post(String callbackUrl, MultivaluedMap payload){
        WebTarget webTarget
                = client.target(callbackUrl);

        webTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.form( payload));
    }
}
