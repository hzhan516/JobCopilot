package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import edu.asu.ser594.resumeassistant.domain.job.event.JobProcessRequestEvent;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqJobEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMqJobEventPublisher publisher;

    @Test
    void publishJobProcessRequest_ShouldConvertAndSendToRabbitMq() {
        JobProcessRequestEvent event = new JobProcessRequestEvent("job-1", "http://example.com", true);

        publisher.publishJobProcessRequest(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_TOPIC,
                RabbitMqConfig.ROUTING_KEY_JOB_PROCESS_REQ,
                event
        );
    }
}
