package edu.asu.ser594.resumeassistant.api.job.facade;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;

/**
 * Inbound port facade for Job related operations.
 */
public interface JobFacade {

    /**
     * Submits a new job posting to be processed and parsed.
     * 
     * @param userId The ID of the user submitting the job.
     * @param request The job submission details containing URL and options.
     * @return The initial state of the job as a response.
     */
    JobResponse submitJob(String userId, SubmitJobRequest request);

    /**
     * Retrieves the current processing status and details of a job.
     * 
     * @param jobId The unique ID of the job.
     * @param userId The ID of the requesting user (for authorization).
     * @return The current state and details of the job.
     */
    JobResponse getJob(String jobId, String userId);
}
