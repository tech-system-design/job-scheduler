package services;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import services.requests.JobRegistrationRequest;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("v1/job/register")
public class JobRegistrationResource {

    private AbstractDataStoreService abstractDataStoreService;
    @Inject
    public JobRegistrationResource(AbstractDataStoreService abstractDataStoreService){
        this.abstractDataStoreService = abstractDataStoreService;
    }

    @POST
    @Path("/")
    public void registerJob(@Valid JobRegistrationRequest jobRegistrationRequest){
        //log.info("Registering a new job");
        abstractDataStoreService.registerJob();
    }

    @POST
    @Path("/cancel/{jobId}")
    public void registerJob(@Valid String jobId){
       // log.info("Registering a new job");
        abstractDataStoreService.cancelJob(jobId);
    }


}
