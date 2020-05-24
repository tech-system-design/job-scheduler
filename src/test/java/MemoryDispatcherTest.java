import job.dispatcher.MemoryDispatcher;
import job.dispatcher.http.HttpRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import models.JobContext;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.util.LinkedList;

import static org.mockito.Mockito.*;

public class MemoryDispatcherTest {

    private JobContext jobContext1, jobContext2;
    private MemoryDispatcher memoryDispatcher;
    private HttpRequest httpRequest;
    private MultivaluedMap multivaluedMap;
    private LinkedList<JobContext> jobContextList;

    @Before
    public void setup(){
        jobContextList = new LinkedList<JobContext>();
        httpRequest = mock(HttpRequest.class);
        memoryDispatcher = new MemoryDispatcher(jobContextList, httpRequest);


        MultivaluedMap multivaluedMap = new MultivaluedHashMap();
        multivaluedMap.add("a", "1");
        multivaluedMap.add("b", "2");
        jobContext1 = JobContext.builder()
                .jobId("abc123")
                .callbackUrl("http.example.com")
                .nextScheduleEpoch(1590310113L)
                .payload(multivaluedMap)
                .build();

        jobContext2 = JobContext.builder()
                .jobId("xyz789")
                .callbackUrl("http.example.com")
                .nextScheduleEpoch(1590310213L)
                .payload(multivaluedMap)
                .build();
    }

    @Test
    public void insertTest(){
        memoryDispatcher.insert(jobContext1);
        memoryDispatcher.insert(jobContext2);
        Assert.assertEquals(memoryDispatcher.getJobContextList().size(), 2);
    }

    @Test
    public void deleteTest(){
        insertTest();
        memoryDispatcher.delete(jobContext1);
        Assert.assertEquals(memoryDispatcher.pendingJobs(), 1);
    }

    @Test
    public void dispatchTest() throws InterruptedException {
        doNothing().when(httpRequest).post("sample-url", multivaluedMap);
        memoryDispatcher.dispatch();
    }
}
