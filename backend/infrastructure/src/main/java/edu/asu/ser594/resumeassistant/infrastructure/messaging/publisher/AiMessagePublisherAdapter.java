package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobRankCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import com.rabbitmq.client.AMQP;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessagePublisherAdapter implements AiMessagePublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendResumeForParsing(ResumeParseCommand command) {
        log.info("Publishing ResumeParseCommand to RabbitMQ for resume: {}", command.resumeId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_RESUME_PARSE,
                command
        );
    }

    @Override
    public void sendTextForVectorGeneration(VectorGenCommand command) {
        log.info("Publishing VectorGenCommand to RabbitMQ for entity: {}", command.referenceId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_VECTOR_GEN,
                command
        );
    }

    @Override
    public void sendJobForParsing(JobParseCommand command) {
        log.info("Publishing JobParseCommand to RabbitMQ for job: {}", command.jobId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_JOB_PARSE,
                command
        );
    }

    @Override
    public void sendConversationRequest(ConversationRequestCommand command) {
        log.info("Publishing ConversationRequestCommand directly to queue for conversation: {}", command.conversationId());
        rabbitTemplate.execute(channel -> {
            var before = channel.queueDeclarePassive(RabbitMqConfig.QUEUE_REQ_CONVERSATION);
            log.info(
                    "RabbitMQ queue state before publish: queue={}, ready={}, consumers={}",
                    RabbitMqConfig.QUEUE_REQ_CONVERSATION,
                    before.getMessageCount(),
                    before.getConsumerCount()
            );

            byte[] body = objectMapper.writeValueAsBytes(command);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

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
