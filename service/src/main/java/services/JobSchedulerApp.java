package services;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import registration.JobRegistrationResource;

public class JobSchedulerApp extends Application<JobSchedulerConfig> {

    public static void main(String[] args) throws Exception {
        new JobSchedulerApp().run(args);
    }

    public void initialize(Bootstrap<JobSchedulerConfig> bootstrap) {

    }

    public void run(JobSchedulerConfig jobSchedulerConfig, Environment environment) throws Exception {
        Injector injector = Guice.createInjector(new JobRegistrationModule());
        final JerseyEnvironment jersey = environment.jersey();
        jersey.register(injector.getInstance(JobRegistrationResource.class));
        System.out.println("Job scheduler app started");
    }
}
