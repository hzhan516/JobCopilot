package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.api.job.dto.request.SubmitJobRequest;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.job.service.ScreenshotValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles the initial job submission flow: validation and persistence.
 * MQ publication is intentionally left to the orchestrator (JobApplicationService)
 * so that the message is sent only after the DB transaction commits.
 * 处理初始职位提交流程：校验与持久化。MQ 发布由编排器负责，
 * 确保消息仅在数据库事务提交后才发出，避免分布式不一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobSubmissionService {

    private final JobRepository jobRepository;

    @Transactional(timeout = 30)
    public Job submit(UUID userId, SubmitJobRequest request) {
        log.info("Submitting new job for async processing for user: {}", userId);
        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Job URL is required / 职位 URL 不能为空");
        }
        ScreenshotValidator.validate(request.screenshotBase64());

        Job job = Job.create(userId, request.url(), request.imageCheckEnabled());
        job.markScraping();
        jobRepository.save(job);
        return job;
    }
}
