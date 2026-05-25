package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.api.job.dto.request.SubmitJobRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobResponse;
import io.jobcopilot.resumeassistant.application.job.mapper.JobResponseMapper;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.service.ScreenshotValidator;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles the initial job submission flow: validation, persistence,
 * and async parse command publication.
 * 处理初始职位提交流程：校验、持久化及异步解析命令发布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobSubmissionService {

    private final JobRepository jobRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;

    @Transactional
    public JobResponse submit(UUID userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);
        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Job URL is required / 职位 URL 不能为空");
        }
        ScreenshotValidator.validate(request.screenshotBase64());

        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        jobRepository.save(job);

        try {
            job.markParsing();
            jobRepository.save(job);
            aiMessagePublisherPort.sendJobForParsing(new JobParseCommand(
                    job.getId(), job.getOriginalUrl(), job.isImageCheckEnabled(), request.screenshotBase64()));
            return JobResponseMapper.toResponse(job);
        } catch (Exception e) {
            log.error("Failed to publish job processing request: {}", job.getId(), e);
            job.markFailed("Failed to publish job processing request: " + e.getMessage());
            jobRepository.save(job);
            throw new RuntimeException("Failed to submit job: " + e.getMessage(), e);
        }
    }
}
