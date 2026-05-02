package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Outbox 清理调度器测试
 * Outbox cleanup scheduler tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox Cleanup Scheduler Tests")
class OutboxCleanupSchedulerTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @InjectMocks
    private OutboxCleanupScheduler scheduler;

    @Test
    @DisplayName("Should delete sent records older than 7 days")
    void shouldDeleteSentRecordsOlderThan7Days() {
        // 执行 / When
        scheduler.cleanup();

        // 验证 / Then
        verify(outboxMessageRepository).deleteByStatusAndSentAtBefore(
                eq(OutboxStatus.SENT),
                any(LocalDateTime.class)
        );
    }
}
