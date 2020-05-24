package registration.ValidationService;

import registration.requests.JobRegistrationRequest;

public interface IValidation {

    public ErrorCode validateJobRequest(JobRegistrationRequest request);
}
