package edu.asu.ser594.resumeassistant.trigger.listener.ai;

import edu.asu.ser594.resumeassistant.api.job.facade.JobFacade;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.EmbeddingRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResultMessageListener {

    private final JobFacade jobFacade;
    private final ResumeFacade resumeFacade;
    private final EmbeddingRepository embeddingRepository;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_RES_JOB_PARSE)
    public void onJobParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for JOB_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            jobFacade.handleJobProcessResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for JOB_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_RES_RESUME_PARSE)
    public void onResumeParseResult(AiResultEvent event) {
        log.info("Received AiResultEvent for RESUME_PARSE, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            resumeFacade.handleParseResult(event);
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for RESUME_PARSE referenceId: {}", event.referenceId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @RabbitListener(queues = RabbitMqConfig.QUEUE_RES_VECTOR_GEN)
    public void onVectorGenResult(AiResultEvent event) {
        log.info("Received AiResultEvent for VECTOR_GEN, referenceId: {}, status: {}", event.referenceId(), event.status());
        try {
            String entityType = event.eventType() != null ? event.eventType() : extractEntityType(event);
            List<Double> embedding = extractEmbedding(event);

            if ("COMPLETED".equals(event.status()) && embedding != null) {
                saveCompletedVector(event.referenceId(), entityType, embedding);
            } else {
                saveFailedVector(event.referenceId(), entityType, event.errorMessage());
            }

        } catch (Exception e) {
            log.error("Error processing AiResultEvent for VECTOR_GEN referenceId: {}", event.referenceId(), e);
        }
    }

    private String extractEntityType(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("entityType")) {
            return (String) event.data().get("entityType");
        }
        // 默认回退 / Default fallback
        return "JOB";
    }

    @SuppressWarnings("unchecked")
    private List<Double> extractEmbedding(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("embedding")) {
            Object rawEmbedding = event.data().get("embedding");
            if (rawEmbedding instanceof List<?> list) {
                List<Double> result = new ArrayList<>(list.size());
                for (Object val : list) {
                    if (val instanceof Number n) {
                        result.add(n.doubleValue());
                    }
                }
                return result;
            }
        }
        return null;
    }

    private void saveCompletedVector(String referenceId, String entityType, List<Double> embedding) {
        if ("RESUME".equalsIgnoreCase(entityType) || "RESUME_VECTOR".equalsIgnoreCase(entityType)) {
            embeddingRepository.saveResumeVector(referenceId, embedding, "COMPLETED", null);
            log.info("Saved COMPLETED resume vector for versionId: {}", referenceId);
        } else {
            embeddingRepository.saveJobVector(referenceId, embedding, "COMPLETED", null);
            log.info("Saved COMPLETED job vector for jobId: {}", referenceId);
        }
    }

    private void saveFailedVector(String referenceId, String entityType, String errorMessage) {
        if ("RESUME".equalsIgnoreCase(entityType) || "RESUME_VECTOR".equalsIgnoreCase(entityType)) {
            embeddingRepository.saveResumeVector(referenceId, null, "FAILED", errorMessage);
            log.warn("Saved FAILED resume vector for versionId: {}", referenceId);
        } else {
            embeddingRepository.saveJobVector(referenceId, null, "FAILED", errorMessage);
            log.warn("Saved FAILED job vector for jobId: {}", referenceId);
        }
    }
}
