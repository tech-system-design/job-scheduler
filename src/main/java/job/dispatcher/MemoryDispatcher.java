package job.dispatcher;

import job.dispatcher.http.HttpRequest;
import lombok.Getter;
import lombok.Synchronized;
import models.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;

@Getter
public class MemoryDispatcher implements IDispatcher{
    //private AbstractDataStore abstractDataStore;
    private HttpRequest httpRequest;
    private LinkedList<JobContext> jobContextList;
    private Logger log = LoggerFactory.getLogger(MemoryDispatcher.class.getName());

    @Inject
    public MemoryDispatcher(LinkedList<JobContext> jobContextList, HttpRequest httpRequest){
        this.jobContextList = jobContextList;
        this.httpRequest = httpRequest;
    }

    public LinkedList<JobContext> getJobContextList(){
        return jobContextList;
    }

    public void insert(JobContext newJob) {
        log.info("Inserting job {} in the dispatcher", newJob.getJobId());

        if(jobContextList.isEmpty()){
            jobContextList.addFirst(newJob);
            return;
        }

        int index = 0;
        JobContext start = jobContextList.get(index);

        if(start.getNextScheduleEpoch() > newJob.getNextScheduleEpoch()){
            jobContextList.addFirst(newJob);
        }
        else {
            while (index < jobContextList.size()-1 && start.getNextScheduleEpoch() < newJob.getNextScheduleEpoch()) {
                start = jobContextList.get(++index);
            }
            jobContextList.add(index, newJob);
        }
    }

    public void delete(JobContext job) {
        if(jobContextList.isEmpty()){
            return;
        }
        log.info("Deleting the job {}", job.getJobId());
        jobContextList.remove(job);
    }

    @Synchronized
    public void dispatch() throws InterruptedException {
        while (true) {
            JobContext currentJobExecutionContext = jobContextList.getFirst();
            if(currentJobExecutionContext != null) {
                if (currentJobExecutionContext.getNextScheduleEpoch() >= System.currentTimeMillis()) {
                    delete(currentJobExecutionContext);
                    log.info("Dispatching the job {}", currentJobExecutionContext.getJobId());
                    httpRequest.post(currentJobExecutionContext.getCallbackUrl(), currentJobExecutionContext.getPayload());
                    //updateDatastore(currentJobExecutionContext);
                }
                else {
                    Thread.sleep(1);
                }
            }
        }
    }

    public int pendingJobs() {
        return jobContextList.size();
    }
}
