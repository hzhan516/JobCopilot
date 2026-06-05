package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.exception.JobException;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates pure DB operations for job result processing.
 * HTTP calls (vector generation) must NOT happen inside this service.
 * 封装职位结果处理的纯数据库操作。HTTP 调用（向量生成）禁止在此服务内执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class JobResultTransactionService {

    private final JobRepository jobRepository;
    private final JobDatasetSyncService jobDatasetSyncService;

    /**
     * Processes the AI result: updates job status and syncs to training dataset.
     * Returns true if the job was successfully marked COMPLETED.
     *
     * @param event AI result event
     * @return true if job status = COMPLETED after processing
     */
    @Transactional(timeout = 60) // dataset sync + save; 60s to accommodate batch-ish writes
    public boolean process(AiResultEvent event) {
        Job job = jobRepository.findById(event.referenceId())
                .orElseThrow(() -> new JobException("job.not.found"));

        boolean completed = false;
        if ("COMPLETED".equals(event.status()) && event.data() != null) {
            try {
                job.markCompleted(jobDatasetSyncService.mapToParsedContent(event.data()));
            } catch (Exception e) {
                job.markFailed("Failed to deserialize AI result data");
                jobRepository.save(job);
                log.error("Deserialization error for job {}: ", event.referenceId(), e);
                return false;
            }
            jobDatasetSyncService.sync(job, event);
            completed = true;
        } else {
            job.markFailed(event.errorMessage() != null
                    ? event.errorMessage()
                    : "Unknown AI processing error");
        }
        jobRepository.save(job);
        log.info("Job {} updated to status {}", job.getId(), job.getStatus());
        return completed;
    }
}
