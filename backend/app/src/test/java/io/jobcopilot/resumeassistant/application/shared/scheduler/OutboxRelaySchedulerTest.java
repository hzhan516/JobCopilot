package io.jobcopilot.resumeassistant.application.shared.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.argThat;

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

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxRelayScheduler scheduler;

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
    @DisplayName("Should mark as failed when rabbit template throws")
    void shouldMarkAsFailedWhenRabbitTemplateThrows() throws Exception {
        // 准备 / Given
        OutboxMessage msg = OutboxMessage.createPending("ex", "rk", "payload");
        when(outboxMessageRepository.findByStatus(OutboxStatus.PENDING))
                .thenReturn(List.of(msg));
        when(objectMapper.readValue(any(String.class), eq(Object.class)))
                .thenReturn("parsedPayload");
        doThrow(new RuntimeException("MQ down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), ArgumentMatchers.<Object>any());
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        scheduler.relayPendingMessages();

        // 验证 / Then — 不应抛出异常，且消息应被标记为 FAILED
        verify(rabbitTemplate).convertAndSend(eq("ex"), eq("rk"), eq("parsedPayload"));
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
