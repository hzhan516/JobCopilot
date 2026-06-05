package io.jobcopilot.resumeassistant.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobRankCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Should save resume parse command to outbox / 应将简历解析命令保存到发件箱")
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
    @DisplayName("Should save job parse command to outbox / 应将职位解析命令保存到发件箱")
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
    @DisplayName("Should save conversation request to outbox / 应将对话请求保存到发件箱")
    void sendConversationRequest_ShouldSaveToOutbox() throws Exception {
        // 准备 / Given
        ConversationRequestCommand command = new ConversationRequestCommand(
                "conv-1", "user-1", new ArrayList<>(), "hello", new ArrayList<>(), null,
                null, null, null, false, "en", "req-1"
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
    @DisplayName("Should save job rank command to outbox / 应将职位排名命令保存到发件箱")
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
