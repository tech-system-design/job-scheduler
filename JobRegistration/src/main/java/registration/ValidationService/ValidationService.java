package registration.ValidationService;

import org.apache.commons.validator.routines.UrlValidator;
import registration.requests.JobRegistrationRequest;

public class ValidationService implements IValidation {

    public ErrorCode validateJobRequest(JobRegistrationRequest request) {
        if(isValidUrl(request.getCallBackUrl()) == ErrorCode.Valid)
        {
            return isValidTime(request.getStartDate(),request.getEndDate());
        }
        return ErrorCode.InvalidUrl;
    }

    private ErrorCode isValidTime(long startTime, long endTime)
    {
        long currentDateTime = System.currentTimeMillis();
        if(startTime < currentDateTime)
        {
            return ErrorCode.InvalidStartDate;
        }

        if(endTime < startTime)
        {
            return  ErrorCode.InvalidEndDate;
        }
        return ErrorCode.Valid;
    }

    private ErrorCode isValidUrl(String url)
    {
        String[] schemes = {"http","https"};
        UrlValidator urlValidator  = new UrlValidator(schemes);
        return urlValidator.isValid(url) ? ErrorCode.Valid : ErrorCode.InvalidUrl;
    }
}
