package edu.asu.ser594.resumeassistant.application.shared.scheduler;

import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Outbox 转发调度器测试
 * Outbox relay scheduler tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox Relay Scheduler Tests")
class OutboxRelaySchedulerTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxRelayScheduler scheduler;

    @Test
    @DisplayName("Should relay pending messages and mark as sent")
    void shouldRelayPendingMessagesAndMarkAsSent() {
        // 准备 / Given
        OutboxMessage msg1 = OutboxMessage.createPending("ex", "rk", "payload1");
        OutboxMessage msg2 = OutboxMessage.createPending("ex", "rk", "payload2");
        when(outboxMessageRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(List.of(msg1, msg2));
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        scheduler.relayPendingMessages();

        // 验证 / Then
        verify(rabbitTemplate, times(2)).convertAndSend(eq("ex"), eq("rk"), any(String.class));
        verify(outboxMessageRepository, times(2)).save(any(OutboxMessage.class));
    }

    @Test
    @DisplayName("Should mark as failed when rabbit template throws")
    void shouldMarkAsFailedWhenRabbitTemplateThrows() {
        // 准备 / Given
        OutboxMessage msg = OutboxMessage.createPending("ex", "rk", "payload");
        when(outboxMessageRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(List.of(msg));
        doThrow(new RuntimeException("MQ down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        scheduler.relayPendingMessages();

        // 验证 / Then — 不应抛出异常，且消息应被标记为 FAILED
        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rk"), eq("payload"));
        verify(outboxMessageRepository).save(argThat(m -> m.getStatus() == OutboxStatus.FAILED));
    }

    @Test
    @DisplayName("Should do nothing when no pending messages")
    void shouldDoNothingWhenNoPendingMessages() {
        // 准备 / Given
        when(outboxMessageRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(Collections.emptyList());

        // 执行 / When
        scheduler.relayPendingMessages();

        // 验证 / Then
        verifyNoInteractions(rabbitTemplate);
        verify(outboxMessageRepository, never()).save(any());
    }
}
