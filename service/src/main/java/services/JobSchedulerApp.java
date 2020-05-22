package services;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class JobSchedulerApp extends Application<JobSchedulerConfig> {

    public static void main(String[] args) throws Exception {
        new JobSchedulerApp().run(args);
    }

    public void run(JobSchedulerConfig jobSchedulerConfig, Environment environment) throws Exception {
        System.out.println("Job scheduler app started");
    }
}
