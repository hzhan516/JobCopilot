package edu.asu.ser594.resumeassistant.infrastructure.messaging.publisher;

import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.JobParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * AI 消息发布适配器测试 / AI message publisher adapter tests
 */
@ExtendWith(MockitoExtension.class)
class AiMessagePublisherAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AiMessagePublisherAdapter publisher;

    @Test
    void sendResumeForParsing_ShouldConvertAndSend() {
        // 准备 / Given
        ResumeParseCommand command = new ResumeParseCommand("resume-1", "http://url", "pdf");

        // 执行 / When
        publisher.sendResumeForParsing(command);

        // 验证 / Then
        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_RESUME_PARSE,
                command
        );
    }

    @Test
    void sendTextForVectorGeneration_ShouldConvertAndSend() {
        // 准备 / Given
        VectorGenCommand command = new VectorGenCommand("ref-1", "JOB", "text");

        // 执行 / When
        publisher.sendTextForVectorGeneration(command);

        // 验证 / Then
        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_VECTOR_GEN,
                command
        );
    }

    @Test
    void sendJobForParsing_ShouldConvertAndSend() {
        // 准备 / Given
        JobParseCommand command = new JobParseCommand("job-1", "http://url", true);

        // 执行 / When
        publisher.sendJobForParsing(command);

        // 验证 / Then
        verify(rabbitTemplate).convertAndSend(
                RabbitMqConfig.EXCHANGE_AI_DIRECT,
                RabbitMqConfig.ROUTING_KEY_REQ_JOB_PARSE,
                command
        );
    }

    @Test
    void sendConversationRequest_ShouldExecuteChannelCallback() {
        // 准备 / Given
        ConversationRequestCommand command = new ConversationRequestCommand(
                "conv-1", "user-1", new ArrayList<>(), "hello", new ArrayList<>(), null,
                null, null, null, false, "en"
        );

        // 执行 / When
        publisher.sendConversationRequest(command);

        // 验证 / Then
        verify(rabbitTemplate).execute(any());
    }
}
