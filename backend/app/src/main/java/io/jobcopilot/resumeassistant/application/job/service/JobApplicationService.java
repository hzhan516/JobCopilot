package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.api.job.dto.request.JobScoreRequest;
import io.jobcopilot.resumeassistant.api.job.dto.request.SubmitJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.request.UpdateJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreHistoryResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreResponse;
import io.jobcopilot.resumeassistant.application.job.mapper.JobResponseMapper;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.exception.JobException;
import io.jobcopilot.resumeassistant.domain.job.port.AiScoringPort;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.repository.JobScoreRepository;
import io.jobcopilot.resumeassistant.domain.job.service.VectorSimilarityService;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.UserFeedbackCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thin orchestrator for the job bounded context. Delegates to domain services
 * and enforces transaction boundaries. All HTTP calls are routed through ports.
 * 职位限界上下文的薄层编排器。委托给领域服务并强制事务边界。所有 HTTP 调用均通过端口路由。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobRepository jobRepository;
    private final JobScoreRepository jobScoreRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorGenerationService vectorGenerationService;
    private final VectorSimilarityService vectorSimilarityService;
    private final AiScoringPort aiScoringPort;
    private final JobDatasetSyncService jobDatasetSyncService;
    private final JobScoringContextLoader scoringContextLoader;
    private final JobScoringResultSaver scoringResultSaver;
    private final JobAccessControl jobAccessControl;
    private final JobSubmissionService jobSubmissionService;
    private final JobResultTransactionService jobResultTxService;

    /**
     * Submits a job. MQ publication happens outside the transaction
     * so the message is sent only after the DB commit succeeds.
     */
    public JobResponse submitJob(UUID userId, SubmitJobRequest request) {
        Job job = jobSubmissionService.submit(userId, request);
        try {
            job.markParsing();
            jobRepository.save(job);
            aiMessagePublisherPort.sendJobForParsing(new io.jobcopilot.resumeassistant.domain.shared.event.ai.JobParseCommand(
                    job.getId(), job.getOriginalUrl(), job.isImageCheckEnabled(), request.screenshotBase64()));
        } catch (Exception e) {
            log.error("Failed to publish job processing request: {}", job.getId(), e);
            job.markFailed("Failed to publish job processing request: " + e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to submit job: " + e.getMessage(), e);
        }
        return io.jobcopilot.resumeassistant.application.job.mapper.JobResponseMapper.toResponse(job);
    }

    /**
     * Processes AI parsing result. Vector generation (HTTP) happens outside the
     * transaction to prevent long-lived DB connections.
     */
    public void handleJobProcessResult(AiResultEvent event) {
        boolean completed = jobResultTxService.process(event);
        if (completed && event.data() != null) {
            ParsedJobContent pc = jobDatasetSyncService.mapToParsedContent(event.data());
            vectorGenerationService.generateForJob(
                    UUID.fromString(event.referenceId()),
                    jobDatasetSyncService.buildVectorText(pc));
        }
    }

    @Transactional(readOnly = true, timeout = 30)
    public JobResponse getJob(String jobId, UUID userId) {
        return JobResponseMapper.toResponse(jobAccessControl.requireAccessible(jobId, userId));
    }

    @Transactional(readOnly = true, timeout = 30)
    public List<JobResponse> listJobs(UUID userId) {
        return jobRepository.findAllByUserId(userId).stream()
                .map(JobResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(timeout = 30)
    public JobResponse updateJob(String jobId, UUID userId, UpdateJobRequest request) {
        Job job = jobAccessControl.requireAccessible(jobId, userId);
        job.updateParsedContent(new ParsedJobContent(
                request.title(), request.company(), request.salary(),
                request.location(), request.description(), request.requirements()));
        jobRepository.save(job);
        log.info("Job {} parsed content updated by user {}", jobId, userId);
        return JobResponseMapper.toResponse(job);
    }

    @Transactional(timeout = 30)
    public void deleteJob(String jobId, UUID userId) {
        Job job = jobAccessControl.requireOwned(jobId, userId);
        job.hide();
        jobRepository.save(job);
        log.info("Job {} hidden by user {}", jobId, userId);
    }

    /**
     * Scores a job against a resume. The AI call is performed outside the
     * transactional boundary to prevent long-lived DB locks.
     */
    public JobScoreResponse scoreJob(String jobId, UUID userId, JobScoreRequest request) {
        JobScoringContextLoader.ScoringContext ctx = scoringContextLoader.load(jobId, userId, request);
        Float semanticMatch = vectorSimilarityService.calculate(request.resumeVersionId(), jobId);
        Map<String, Object> aiResponse = aiScoringPort.score(
                jobId, request.resumeVersionId(), ctx.resume(), ctx.job(), semanticMatch);
        return scoringResultSaver.save(jobId, userId, request.resumeVersionId(), aiResponse);
    }

    @Transactional(readOnly = true, timeout = 30)
    public List<JobScoreHistoryResponse> getScoreHistory(UUID userId) {
        return jobScoreRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JobResponseMapper::toScoreHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(timeout = 30)
    public void trackUserAction(String jobId, UUID userId, String actionType, String resumeVersionId) {
        Job job = jobAccessControl.requireOwned(jobId, userId);
        try {
            aiMessagePublisherPort.sendUserFeedback(new UserFeedbackCommand(
                    UUID.randomUUID().toString(), userId, resumeVersionId, jobId,
                    actionType, 0.0, "{}", java.time.Instant.now()));
            log.info("User action '{}' tracked for jobId={}, resumeVersionId={}",
                    actionType, jobId, resumeVersionId);
        } catch (Exception e) {
            log.error("Failed to send user action '{}' to outbox for jobId={}: {}",
                    actionType, jobId, e.getMessage());
        }
    }
}


