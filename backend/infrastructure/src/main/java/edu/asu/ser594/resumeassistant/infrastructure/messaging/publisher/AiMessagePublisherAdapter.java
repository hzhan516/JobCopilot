package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.*;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 消息发布适配器 / AI message publisher adapter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessagePublisherAdapter implements AiMessagePublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发送简历解析请求 / Send resume parse request
     */
    @Override
    public void sendResumeForParsing(ResumeParseCommand command) {
        log.info("Publishing ResumeParseCommand to RabbitMQ for resume: {}", command.resumeId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_RESUME_PARSE,
                command
        );
    }

    /**
     * 发送文本向量生成请求 / Send text vector generation request
     */
    @Override
    public void sendTextForVectorGeneration(VectorGenCommand command) {
        log.info("Publishing VectorGenCommand to RabbitMQ for entity: {}", command.referenceId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_VECTOR_GEN,
                command
        );
    }

    /**
     * 发送职位解析请求 / Send job parse request
     */
    @Override
    public void sendJobForParsing(JobParseCommand command) {
        log.info("Publishing JobParseCommand to RabbitMQ for job: {}", command.jobId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_JOB_PARSE,
                command
        );
    }

    /**
     * 发送对话请求（使用 AMQP 通道确认模式） / Send conversation request (using AMQP channel confirm mode)
     */
    @Override
    public void sendConversationRequest(ConversationRequestCommand command) {
        log.info("Publishing ConversationRequestCommand directly to queue for conversation: {}", command.conversationId());
        rabbitTemplate.execute(channel -> {
            // 查询队列状态 / Query queue state
            var before = channel.queueDeclarePassive(RabbitMqConfig.QUEUE_REQ_CONVERSATION);
            log.info(
                    "RabbitMQ queue state before publish: queue={}, ready={}, consumers={}",
                    RabbitMqConfig.QUEUE_REQ_CONVERSATION,
                    before.getMessageCount(),
                    before.getConsumerCount()
            );

            // 序列化消息体 / Serialize message body
            byte[] body = objectMapper.writeValueAsBytes(command);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            // 启用发布确认并发送 / Enable publisher confirm and send
            channel.confirmSelect();
            channel.basicPublish(
                    "",
                    RabbitMqConfig.QUEUE_REQ_CONVERSATION,
                    true,
                    properties,
                    body
            );
            channel.waitForConfirmsOrDie(5000);
            var after = channel.queueDeclarePassive(RabbitMqConfig.QUEUE_REQ_CONVERSATION);
            log.info(
                    "Published ConversationRequestCommand with AMQP basicPublish to queue: {}, payloadBytes: {}, readyAfter={}, consumersAfter={}",
                    RabbitMqConfig.QUEUE_REQ_CONVERSATION,
                    body.length,
                    after.getMessageCount(),
                    after.getConsumerCount()
            );
            return null;
        });
    }

    /**
     * 发送职位排名请求 / Send job rank request
     */
    @Override
    public void sendJobForRanking(JobRankCommand command) {
        log.info("Publishing JobRankCommand to RabbitMQ for match: {}", command.matchId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_JOB_RANK,
                command
        );
    }
}
