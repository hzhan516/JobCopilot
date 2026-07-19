package io.jobcopilot.resumeassistant.application.shared.scheduler;

import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxMessageRepository repository;
    @Mock
    private OutboxRelayTransactionService transactions;
    @Mock
    private OutboxRabbitPublisher publisher;

    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        OutboxRelayTransactionService relayTransactionService =
                new OutboxRelayTransactionService(outboxMessageRepository, rabbitTemplate, objectMapper);
        scheduler = new OutboxRelayScheduler(outboxMessageRepository, relayTransactionService);
    }

    @Test
    @DisplayName("Should relay pending messages and mark as sent")
    void shouldRelayPendingMessagesAndMarkAsSent() throws Exception {
        // 准备 / Given
        OutboxMessage msg1 = OutboxMessage.createPending("ex", "rk", "payload1");
        OutboxMessage msg2 = OutboxMessage.createPending("ex", "rk", "payload2");
        when(outboxMessageRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(List.of(msg1, msg2));
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.readValue(any(String.class), eq(Object.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        scheduler.relayPendingMessages();

        // 验证 / Then
        verify(rabbitTemplate, times(2)).convertAndSend(eq("ex"), eq("rk"), ArgumentMatchers.<Object>any());
        verify(outboxMessageRepository, times(2)).save(any(OutboxMessage.class));
    }

    @Test
    void confirmedPublishMarksClaimedMessageSent() throws Exception {
        OutboxMessage candidate = OutboxMessage.createPending("ex", "rk", "{}");
        OutboxMessage claimed = claimed(candidate);
        when(repository.findDueForDelivery(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(candidate));
        when(transactions.claim(eq(candidate.getId()), anyString(), any(LocalDateTime.class)))
                .thenReturn(claimed);

        scheduler.relayPendingMessages();

        verify(publisher).publish(claimed, 10L);
        verify(transactions).markSent(candidate.getId());
        verify(transactions, never()).markFailed(anyString(), anyString(), any(), anyInt());
    }

    @Test
    void failedPublishSchedulesBoundedRetry() throws Exception {
        OutboxMessage candidate = OutboxMessage.createPending("ex", "rk", "{}");
        OutboxMessage claimed = claimed(candidate);
        when(repository.findDueForDelivery(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(candidate));
        when(transactions.claim(eq(candidate.getId()), anyString(), any(LocalDateTime.class)))
                .thenReturn(claimed);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(publisher).publish(claimed, 10L);

        scheduler.relayPendingMessages();

        verify(transactions).markFailed(eq(candidate.getId()), eq("BROKER_UNAVAILABLE"),
                any(LocalDateTime.class), eq(5));
        verify(transactions, never()).markSent(anyString());
    }

    @Test
    void staleProcessingClaimsAreRecoveredBeforePolling() {
        OutboxMessage stale = claimed(OutboxMessage.createPending("ex", "rk", "{}"));
        when(repository.findStaleProcessing(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(stale));
        when(repository.findDueForDelivery(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of());

        scheduler.relayPendingMessages();

        verify(transactions).recoverStale(eq(stale.getId()), any(LocalDateTime.class));
    }

    @Test
    void outboxEntityTransitionsToDeadAtMaxAttempt() {
        OutboxMessage message = OutboxMessage.createPending("ex", "rk", "{}");
        LocalDateTime now = LocalDateTime.now();
        for (int attempt = 1; attempt <= 5; attempt++) {
            message.markProcessing("worker", now);
            message.markFailed("BROKER_UNAVAILABLE", now.plusSeconds(2), 5);
        }
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(message.getAttemptCount()).isEqualTo(5);
        assertThat(message.getNextAttemptAt()).isNull();
    }

    private static OutboxMessage claimed(OutboxMessage message) {
        message.markProcessing("worker", LocalDateTime.now());
        return message;
    }
}
