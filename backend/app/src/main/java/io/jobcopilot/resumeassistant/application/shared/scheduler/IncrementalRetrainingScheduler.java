package io.jobcopilot.resumeassistant.application.shared.scheduler;

import io.jobcopilot.resumeassistant.domain.embedding.port.ModelRetrainingPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 增量模型定时重训练调度器 / Incremental model retraining scheduler
 * <p>
 * 每天凌晨 2 点触发 AI Service 的模型权重强制重算，作为增量学习的兜底机制。
 * Triggers AI Service model weight recomputation daily at 2 AM as a safety net for incremental learning.
 * </p>
 * <p>
 * 实际 HTTP 调用被委托给 {@link ModelRetrainingPort}，本调度器仅负责编排和日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementalRetrainingScheduler {

    private final ModelRetrainingPort modelRetrainingPort;

    /**
     * 每天凌晨 2 点调用 AI Service 强制重算模型权重
     * Calls AI Service to force model weight recomputation at 2 AM daily
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "IncrementalRetraining", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    public void triggerIncrementalRetraining() {
        log.info("Triggering incremental model retraining via ModelRetrainingPort");
        try {
            modelRetrainingPort.triggerRetraining();
        } catch (Exception e) {
            log.error("Failed to trigger incremental model retraining: {}", e.getMessage());
            // 非阻塞：定时调度失败不应影响主业务
        }
    }
}
