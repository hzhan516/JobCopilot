package edu.asu.ser594.resumeassistant.application.job;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.application.job.service.JobApplicationService;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 职位门面实现
 * Job Facade Implementation
 */
@Component
@RequiredArgsConstructor
public class JobFacadeImpl implements JobFacade {

    private final JobApplicationService applicationService;

    @Override
    public JobResponse submitJob(final UUID userId, final SubmitJobRequest request) {
        return applicationService.submitJob(userId, request);
    }

    @Override
    public JobResponse getJob(final String jobId, final UUID userId) {
        return applicationService.getJob(jobId, userId);
    }

    @Override
    public List<JobResponse> listJobs(final UUID userId) {
        return applicationService.listJobs(userId);
    }

    @Override
    public void handleJobProcessResult(final AiResultEvent event) {
        applicationService.handleJobProcessResult(event);
    }
}
