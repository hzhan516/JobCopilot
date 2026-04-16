package edu.asu.ser594.resumeassistant.application.job;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.application.job.service.JobApplicationService;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 职位门面实现
 * Job Facade Implementation
 */
@Component
@RequiredArgsConstructor
public class JobFacadeImpl implements JobFacade {

    private final JobApplicationService applicationService;

    @Override
    public JobResponse submitJob(String userId, SubmitJobRequest request) {
        return applicationService.submitJob(userId, request);
    }

    @Override
    public JobResponse getJob(String jobId, String userId) {
        return applicationService.getJob(jobId, userId);
    }

    @Override
    public List<JobResponse> listJobs(String userId) {
        return applicationService.listJobs(userId);
    }

    @Override
    public JobMatchResponse matchJobs(String userId, JobMatchRequest request) {
        return applicationService.matchJobs(userId, request);
    }

    @Override
    public void handleJobProcessResult(AiResultEvent event) {
        applicationService.handleJobProcessResult(event);
    }
}
