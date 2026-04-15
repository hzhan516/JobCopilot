package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessRequestEvent;
import edu.asu.ser594.resumeassistant.domain.job.port.JobEventPublisherPort;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqJobEventPublisher implements JobEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishJobProcessRequest(JobProcessRequestEvent event) {
        log.info("Publishing JobProcessRequestEvent to RabbitMQ for job: {}", event.jobId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_TOPIC,
                RabbitMqConfig.ROUTING_KEY_JOB_PROCESS_REQ,
                event
        );
    }
}
