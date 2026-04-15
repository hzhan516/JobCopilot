package edu.asu.ser594.resumeassistant.api.job.facade;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessResultEvent;

/**
 * Inbound port facade for Job related operations.
 */
public interface JobFacade {

    JobResponse submitJob(String userId, SubmitJobRequest request);

    JobResponse getJob(String jobId, String userId);

    void handleJobProcessResult(JobProcessResultEvent event);
}
