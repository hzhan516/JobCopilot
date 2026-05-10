package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Outbox 消息清理调度器
 * Outbox message cleanup scheduler
 * <p>
 * 定期清理已发送超过保留期的 Outbox 记录，防止表无限膨胀。
 * Periodically cleans up sent outbox records older than the retention period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private static final int RETENTION_DAYS = 7;
    private final OutboxMessageRepository outboxMessageRepository;

    /**
     * 每天凌晨 3:00 执行清理
     * Cleanup at 3:00 AM every day
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "OutboxCleanup", lockAtMostFor = "30m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("Starting outbox cleanup for records sent before: {}", cutoff);

        try {
            outboxMessageRepository.deleteByStatusAndSentAtBefore(OutboxStatus.SENT, cutoff);
            log.info("Outbox cleanup completed for records sent before: {}", cutoff);
        } catch (Exception e) {
            log.error("Failed to cleanup outbox messages sent before: {}", cutoff, e);
        }
    }
}
