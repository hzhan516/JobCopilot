package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.domain.shared.entity.OutboxMessage;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.*;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.repository.OutboxMessageRepository;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
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
     * 发送文本向量生成请求 / Send text vector generation request
     */
    @Override
    public void sendTextForVectorGeneration(VectorGenCommand command) {
        log.info("Saving VectorGenCommand to outbox for entity: {}", command.referenceId());
        saveToOutbox(RabbitMqConfig.ROUTING_KEY_REQ_VECTOR_GEN, command);
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
