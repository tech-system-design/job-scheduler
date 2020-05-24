package registration;

import registration.ValidationService.ValidationService;

public class AbstractDataStoreService implements IDataStore{
    public void registerJob(){
        ValidationService validationService = new ValidationService();
    }

    public void schedulerJob() {

    }

    public void cancelJob() {

    }

    public void cancelJob(String jobId) {

    }

    public void getJobStatus() {

    }

    public void handleHotJob() {

    }
}
