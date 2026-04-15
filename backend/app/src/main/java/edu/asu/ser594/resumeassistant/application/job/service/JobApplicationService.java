package edu.asu.ser594.resumeassistant.application.job.service;

import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.service.LlmParserPort;
import edu.asu.ser594.resumeassistant.domain.job.service.VisionVerificationPort;
import edu.asu.ser594.resumeassistant.domain.job.service.WebScraperPort;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ScrapeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService implements JobFacade {

    private final JobRepository jobRepository;
    private final WebScraperPort webScraperPort;
    private final LlmParserPort llmParserPort;
    private final VisionVerificationPort visionVerificationPort;

    @Override
    @Transactional
    public JobResponse submitJob(String userId, SubmitJobRequest request) {
        log.info("Submitting new job for user: {}", userId);
        
        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job = jobRepository.save(job);

        try {
            job.markScraping();
            jobRepository.save(job);
            ScrapeResult scrapeResult = webScraperPort.scrape(job.getOriginalUrl(), job.isImageCheckEnabled());

            job.markParsing();
            jobRepository.save(job);
            ParsedJobContent parsedContent = llmParserPort.parse(scrapeResult.markdownText());

            if (job.isImageCheckEnabled() && scrapeResult.screenshotUrl() != null) {
                log.info("Performing vision verification for job: {}", job.getId());
                parsedContent = visionVerificationPort.verifyAndFix(parsedContent, scrapeResult.screenshotUrl());
            }

            job.markCompleted(parsedContent);
            jobRepository.save(job);
            
            return mapToResponse(job);

        } catch (Exception e) {
            log.error("Failed to process job: {}", job.getId(), e);
            job.markFailed(e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to process job: " + e.getMessage(), e);
        }
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
