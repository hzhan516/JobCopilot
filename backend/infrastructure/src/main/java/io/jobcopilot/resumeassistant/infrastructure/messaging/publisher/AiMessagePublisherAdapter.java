package io.jobcopilot.resumeassistant.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.JobRankCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.UserFeedbackCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import io.jobcopilot.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 消息发布适配器 / AI message publisher adapter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessagePublisherAdapter implements AiMessagePublisherPort {

    private final ObjectMapper objectMapper;
    private final OutboxMessageRepository outboxMessageRepository;

    /**
     * 发送简历解析请求 / Send resume parse request
     */
    @Override
    public void sendResumeForParsing(ResumeParseCommand command) {
        log.info("Saving ResumeParseCommand to outbox for resume: {}", command.resumeId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_RESUME_PARSE, command);
    }

    /**
     * 发送职位解析请求 / Send job parse request
     */
    @Override
    public void sendJobForParsing(JobParseCommand command) {
        log.info("Saving JobParseCommand to outbox for job: {}", command.getJobId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_JOB_PARSE, command);
    }

    /**
     * 发送对话请求 / Send conversation request
     */
    @Override
    public void sendConversationRequest(ConversationRequestCommand command) {
        log.info("Saving ConversationRequestCommand to outbox for conversation: {}", command.conversationId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_CONVERSATION, command);
    }

    /**
     * 发送职位排名请求 / Send job rank request
     */
    @Override
    public void sendJobForRanking(JobRankCommand command) {
        log.info("Saving JobRankCommand to outbox for match: {}", command.matchId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_JOB_RANK, command);
    }

    /**
     * 发送评分标签到 AI 服务用于增量模型训练
     * Send score label to AI service for incremental model training
     */
    @Override
    public void sendUserFeedback(UserFeedbackCommand command) {
        log.info("Saving UserFeedbackCommand to outbox for job: {}", command.jobId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_MODEL_INCREMENTAL, command);
    }

    private void saveToOutbox(String routingKey, Object command) {
        try {
            String payload = objectMapper.writeValueAsString(command);
            OutboxMessage message = OutboxMessage.createPending(
                    RabbitMqConfig.EXCHANGE_AI_DIRECT,
                    routingKey,
                    payload
            );
            outboxMessageRepository.save(message);
            log.info("Saved command to outbox: exchange={}, routingKey={}",
                    RabbitMqConfig.EXCHANGE_AI_DIRECT, routingKey);
        } catch (Exception e) {
            log.error("Failed to save command to outbox: routingKey={}", routingKey, e);
            throw new RuntimeException("Failed to save command to outbox", e);
        }
    }
}
