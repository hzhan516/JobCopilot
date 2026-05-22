package io.jobcopilot.resumeassistant.trigger.listener.ai;

import io.jobcopilot.resumeassistant.api.conversation.facade.ConversationFacade;
import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.api.job.dto.response.MatchFactors;
import io.jobcopilot.resumeassistant.api.job.dto.response.MatchItem;
import io.jobcopilot.resumeassistant.api.job.facade.JobFacade;
import io.jobcopilot.resumeassistant.api.matching.facade.MatchingFacade;
import io.jobcopilot.resumeassistant.api.resume.facade.ResumeFacade;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Async inbound gateway for AI service results. Decouples the AI worker from business workflows by
 * delegating outcomes to API-layer facades while swallowing exceptions to avoid poisonous messages.
 * AI 服务结果的异步入口网关。通过将处理结果委托给 API 层门面来解耦 AI 工作线程与业务流，同时捕获异常以避免毒消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResultMessageListener {

    private final JobFacade jobFacade;
    private final ResumeFacade resumeFacade;
    private final ConversationFacade conversationFacade;
    private final VectorFacade vectorFacade;
    private final MatchingFacade matchingFacade;

    @RabbitListener(queues = "${app.rabbitmq.queue.res.job-parse}")
    public void onJobParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for JOB_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            jobFacade.handleJobProcessResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for JOB_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.res.resume-parse}")
    public void onResumeParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for RESUME_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            resumeFacade.handleParseResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for RESUME_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

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
                conversationFacade.failAiReply(event.referenceId(), errorContent);
                return;
            }

            String content = extractReplyContent(event);
            String fileUrl = extractFileUrl(event);
            String aiOptimizedMarkdown = extractAiOptimizedMarkdown(event);

            conversationFacade.saveAiReply(event.referenceId(), content, fileUrl, aiOptimizedMarkdown);
            log.info("Saved AI reply for conversation: {}", event.referenceId());

            conversationFacade.completeAiReply(event.referenceId(), content);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for CONVERSATION_REPLY referenceId: {}", event.referenceId(), e);
            conversationFacade.failAiReply(event.referenceId(), e.getMessage());
        }
    }

    @RabbitListener(queues = "${app.rabbitmq.queue.res.job-rank}")
    public void onJobRankResult(AiResultEvent event) {
        log.info("Received AiResultEvent for JOB_RANK, referenceId: {}, status: {}",
                event.referenceId(), event.status());
        try {
            if (!"COMPLETED".equals(event.status())) {
                log.warn("Job rank failed for matchId: {}, error: {}",
                        event.referenceId(), event.errorMessage());
                return;
            }
            Map<String, Object> data = event.data();
            if (data == null) {
                log.warn("Job rank result has no data for matchId: {}", event.referenceId());
                return;
            }

            Long rankTimeMs = data.containsKey("rankTimeMs")
                    ? ((Number) data.get("rankTimeMs")).longValue() : 0L;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rankedData = (List<Map<String, Object>>) data.get("rankedResults");
            List<MatchItem> matchItems = new ArrayList<>();

            if (rankedData != null) {
                for (Map<String, Object> item : rankedData) {
                    MatchFactors factors = extractMatchFactors(item);
                    matchItems.add(new MatchItem(
                            (String) item.get("jobId"),
                            (String) item.get("title"),
                            (String) item.get("company"),
                            item.get("matchScore") != null ? ((Number) item.get("matchScore")).doubleValue() : 0.0,
                            factors,
                            (String) item.get("description"),
                            (String) item.get("matchReason")
                    ));
                }
            }

            matchingFacade.saveJobRankResult(event.referenceId(), matchItems, rankTimeMs);
            log.info("Job rank result saved for matchId: {}", event.referenceId());
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for JOB_RANK referenceId: {}",
                    event.referenceId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private MatchFactors extractMatchFactors(final Map<String, Object> item) {
        final Object factorsObj = item.get("matchFactors");
        if (factorsObj instanceof Map) {
            final Map<String, Object> factorsMap = (Map<String, Object>) factorsObj;
            return new MatchFactors(
                    extractDouble(factorsMap.get("skillMatch")),
                    extractDouble(factorsMap.get("experienceMatch")),
                    extractDouble(factorsMap.get("locationMatch"))
            );
        }
        return new MatchFactors(0.0, 0.0, 0.0);
    }

    private Double extractDouble(final Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private String extractReplyContent(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("content")) {
            return (String) event.data().get("content");
        }
        return "";
    }

    private String extractFileUrl(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("fileUrl")) {
            return (String) event.data().get("fileUrl");
        }
        return null;
    }

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
