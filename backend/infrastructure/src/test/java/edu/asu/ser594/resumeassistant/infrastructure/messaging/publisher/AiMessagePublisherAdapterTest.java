package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 消息发布适配器测试 / AI message publisher adapter tests
 */
@ExtendWith(MockitoExtension.class)
class AiMessagePublisherAdapterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @InjectMocks
    private AiMessagePublisherAdapter publisher;

    @Test
    void sendResumeForParsing_ShouldSaveToOutbox() throws Exception {
        // 准备 / Given
        ResumeParseCommand command = new ResumeParseCommand("resume-1", "http://url", "pdf");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"resumeId\":\"resume-1\"}");
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        publisher.sendResumeForParsing(command);

        // 验证 / Then
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        OutboxMessage saved = captor.getValue();
        assertThat(saved.getExchange()).isEqualTo(RabbitMqConfig.EXCHANGE_AI_DIRECT);
        assertThat(saved.getRoutingKey()).isEqualTo(RabbitMqConfig.ROUTING_KEY_REQ_RESUME_PARSE);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getPayload()).isEqualTo("{\"resumeId\":\"resume-1\"}");
    }

    @Test
    void sendJobForParsing_ShouldSaveToOutbox() throws Exception {
        // 准备 / Given
        JobParseCommand command = new JobParseCommand("job-1", "http://url", true);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"jobId\":\"job-1\"}");
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        publisher.sendJobForParsing(command);

        // 验证 / Then
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo(RabbitMqConfig.ROUTING_KEY_REQ_JOB_PARSE);
    }

    @Test
    void sendConversationRequest_ShouldSaveToOutbox() throws Exception {
        // 准备 / Given
        ConversationRequestCommand command = new ConversationRequestCommand(
                "conv-1", "user-1", new ArrayList<>(), "hello", new ArrayList<>(), null,
                null, null, null, false, "en"
        );
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"conversationId\":\"conv-1\"}");
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        publisher.sendConversationRequest(command);

        // 验证 / Then
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo(RabbitMqConfig.ROUTING_KEY_REQ_CONVERSATION);
    }

    @Test
    void sendJobForRanking_ShouldSaveToOutbox() throws Exception {
        // 准备 / Given
        JobRankCommand command = new JobRankCommand(
                "match-1", "user-1", "resume-1", "resume text", "query",
                new ArrayList<>(), new java.util.HashMap<>()
        );
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"matchId\":\"match-1\"}");
        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // 执行 / When
        publisher.sendJobForRanking(command);

        // 验证 / Then
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo(RabbitMqConfig.ROUTING_KEY_REQ_JOB_RANK);
    }
}
