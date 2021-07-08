package job.dispatcher;

import models.JobContext;

public interface IDispatcher {
    void insert(JobContext newJob);
    void delete(JobContext newJob);
    void dispatch() throws InterruptedException;
    int pendingJobs();
}
