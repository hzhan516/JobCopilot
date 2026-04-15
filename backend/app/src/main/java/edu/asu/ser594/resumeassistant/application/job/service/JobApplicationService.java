package edu.asu.ser594.resumeassistant.application.job.service;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessRequestEvent;
import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessResultEvent;
import edu.asu.ser594.resumeassistant.domain.job.port.JobEventPublisherPort;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService implements JobFacade {

    private final JobRepository jobRepository;
    private final JobEventPublisherPort jobEventPublisherPort;

    @Override
    @Transactional
    public JobResponse submitJob(String userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);
        
        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        job = jobRepository.save(job);

        try {
            JobProcessRequestEvent event = new JobProcessRequestEvent(
                    job.getId(),
                    job.getOriginalUrl(),
                    job.isImageCheckEnabled()
            );
            jobEventPublisherPort.publishJobProcessRequest(event);
            return mapToResponse(job);
        } catch (Exception e) {
            log.error("Failed to publish job processing request: {}", job.getId(), e);
            job.markFailed("Failed to publish job processing request: " + e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to submit job: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleJobProcessResult(JobProcessResultEvent event) {
        Job job = jobRepository.findById(event.jobId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + event.jobId()));

        if (event.success() && event.parsedContent() != null) {
            job.markCompleted(event.parsedContent());
        } else {
            job.markFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown AI processing error");
        }
        
        jobRepository.save(job);
        log.info("Job {} updated to status {}", job.getId(), job.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(String jobId, String userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (!job.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Job does not belong to user: " + userId);
        }

        return mapToResponse(job);
    }

    private JobResponse mapToResponse(Job job) {
        JobResponse.ParsedJobContentResponse parsedContentResponse = null;
        if (job.getParsedContent() != null) {
            parsedContentResponse = new JobResponse.ParsedJobContentResponse(
                    job.getParsedContent().title(),
                    job.getParsedContent().company(),
                    job.getParsedContent().description(),
                    job.getParsedContent().requirements()
            );
        }

        return new JobResponse(
                job.getId(),
                job.getUserId(),
                job.getOriginalUrl(),
                job.getStatus().name(),
                parsedContentResponse,
                job.isImageCheckEnabled(),
                job.getErrorMessage()
        );
    }
}
