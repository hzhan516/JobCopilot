package edu.asu.ser594.resumeassistant.application.job.service;

import edu.asu.ser594.resumeassistant.api.job.dto.request.JobMatchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.request.SubmitJobRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobMatchResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.JobResponse;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchFactors;
import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobRepository jobRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public JobResponse submitJob(String userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);
        
        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        job = jobRepository.save(job);

        try {
            JobParseCommand command = new JobParseCommand(
                    job.getId(),
                    job.getOriginalUrl(),
                    job.isImageCheckEnabled()
            );
            aiMessagePublisherPort.sendJobForParsing(command);
            return mapToResponse(job);
        } catch (Exception e) {
            log.error("Failed to publish job processing request: {}", job.getId(), e);
            job.markFailed("Failed to publish job processing request: " + e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to submit job: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleJobProcessResult(AiResultEvent event) {
        Job job = jobRepository.findById(event.referenceId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + event.referenceId()));

        if ("COMPLETED".equals(event.status()) && event.data() != null) {
            try {
                ParsedJobContent content = objectMapper.convertValue(event.data(), ParsedJobContent.class);
                job.markCompleted(content);
            } catch (Exception e) {
                job.markFailed("Failed to deserialize AI result data");
                log.error("Deserialization error for job {}: ", event.referenceId(), e);
                jobRepository.save(job);
                return;
            }

            try {
                VectorGenCommand vectorCmd = new VectorGenCommand(
                        job.getId(),
                        "JOB",
                        job.getParsedContent().title() + "\n" + job.getParsedContent().company() + "\n" + job.getParsedContent().description() + "\n" + String.join("\n", job.getParsedContent().requirements())
                );
                aiMessagePublisherPort.sendTextForVectorGeneration(vectorCmd);
                log.info("Triggered async vector generation for job: {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to publish job vector gen request: {}", job.getId(), e);
            }
        } else {
            job.markFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown AI processing error");
        }
        
        jobRepository.save(job);
        log.info("Job {} updated to status {}", job.getId(), job.getStatus());
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(String jobId, String userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (!job.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Job does not belong to user: " + userId);
        }

        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(String userId) {
        List<Job> jobs = jobRepository.findAllByUserId(userId);
        return jobs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public JobMatchResponse matchJobs(String userId, JobMatchRequest request) {
        log.info("Requesting job match for user: {}, query: {}", userId, request.query());
        String url = "http://localhost:8000/api/v1/match";

        try {
            return restTemplate.postForObject(url, request, JobMatchResponse.class);
        } catch (RestClientException e) {
            log.warn("Python AI service unavailable, returning mock data. Error: {}", e.getMessage());
            return buildMockJobMatchResponse();
        }
    }

    private JobMatchResponse buildMockJobMatchResponse() {
        MatchItem mock1 = new MatchItem(
                "mock-job-1",
                "Senior Java Developer",
                "Tech Corp",
                0.92,
                new MatchFactors(0.95, 0.90, 0.88),
                "Looking for an experienced Java developer with Spring Boot skills."
        );
        MatchItem mock2 = new MatchItem(
                "mock-job-2",
                "Backend Engineer",
                "Startup Inc",
                0.85,
                new MatchFactors(0.88, 0.80, 0.90),
                "Join our fast-paced team to build scalable microservices."
        );
        return new JobMatchResponse(java.util.List.of(mock1, mock2), 2, 12L, 45L);
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
