package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 结果消息监听器 / AI result message listener
 * <p>
 * 严格遵循 DDD 分层规范：仅依赖 API 层 Facade 接口，不直接触碰 Domain 或 Infrastructure。
 * 队列名称通过 ${...} 占位符从配置文件中读取，解除对 RabbitMqConfig 配置类的直接依赖。
 * Strictly follows DDD layering: only depends on API-layer Facade interfaces,
 * never directly touches Domain or Infrastructure.
 * Queue names are read from configuration files via ${...} placeholders,
 * decoupling from the RabbitMqConfig class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResultMessageListener {

    private final JobFacade jobFacade;
    private final ResumeFacade resumeFacade;
    private final ConversationFacade conversationFacade;
    private final VectorFacade vectorFacade;

    /**
     * 监听职位解析结果 / Listen for job parse results
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.res.job-parse}")
    public void onJobParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for JOB_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            jobFacade.handleJobProcessResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for JOB_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

    /**
     * 监听简历解析结果 / Listen for resume parse results
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.res.resume-parse}")
    public void onResumeParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for RESUME_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            resumeFacade.handleParseResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for RESUME_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

    /**
     * 监听向量生成结果 / Listen for vector generation results
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.res.vector-gen}")
    public void onVectorGenResult(AiResultEvent event) {
        log.info("Received AiResultEvent for VECTOR_GEN, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            vectorFacade.handleVectorGenResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for VECTOR_GEN referenceId: {}", event.referenceId(), e);
        }
    }

    /**
     * 监听对话回复结果 / Listen for conversation reply results
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.res.conversation}")
    public void onConversationReply(AiResultEvent event) {
        log.info("Received AiResultEvent for CONVERSATION_REPLY, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            if (!"COMPLETED".equals(event.status())) {
                log.warn("Conversation AI reply failed for conversation: {}, error: {}", event.referenceId(), event.errorMessage());
                String errorContent = "AI response failed: " + (event.errorMessage() != null ? event.errorMessage() : "Unknown error");
                conversationFacade.saveAiReply(
                        event.referenceId(),
                        errorContent,
                        null,
                        null
                );
                // 释放等待中的流连接 / Release pending stream connection
                conversationFacade.failAiReply(event.referenceId(), errorContent);
                return;
            }

            // 提取回复内容、文件 URL 和 AI 优化简历 / Extract reply content, file URL and AI optimized resume
            String content = extractReplyContent(event);
            String fileUrl = extractFileUrl(event);
            String aiOptimizedMarkdown = extractAiOptimizedMarkdown(event);

            conversationFacade.saveAiReply(event.referenceId(), content, fileUrl, aiOptimizedMarkdown);
            log.info("Saved AI reply for conversation: {}", event.referenceId());

            // 唤醒等待中的流请求 / Wake up pending stream request
            conversationFacade.completeAiReply(event.referenceId(), content);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for CONVERSATION_REPLY referenceId: {}", event.referenceId(), e);
            conversationFacade.failAiReply(event.referenceId(), e.getMessage());
        }
    }

    // 提取回复内容 / Extract reply content
    private String extractReplyContent(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("content")) {
            return (String) event.data().get("content");
        }
        return "";
    }

    // 提取文件 URL / Extract file URL
    private String extractFileUrl(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("fileUrl")) {
            return (String) event.data().get("fileUrl");
        }
        return null;
    }

    // 提取 AI 优化后的 Markdown / Extract AI optimized markdown
    private String extractAiOptimizedMarkdown(AiResultEvent event) {
        if (event.data() == null) {
            return null;
        }
        Object mod = event.data().get("resumeModification");
        if (mod instanceof Map<?, ?> map) {
            if (Boolean.TRUE.equals(map.get("modified"))) {
                return (String) map.get("markdown");
            }
        }
        return null;
    }
}
