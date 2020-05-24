package registration;

public interface IDataStore {
    void registerJob();
    void schedulerJob();
    void cancelJob();
    void getJobStatus();
    void handleHotJob();
}

