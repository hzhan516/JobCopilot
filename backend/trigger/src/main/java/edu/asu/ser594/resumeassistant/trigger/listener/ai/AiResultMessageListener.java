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
            Map<String, Object> data = event.data();
            List<Double> embedding = null;
            String entityType = "JOB"; // default fallback

            if (data != null) {
                if (data.containsKey("embedding")) {
                    embedding = (List<Double>) data.get("embedding");
                }
                if (data.containsKey("entityType")) {
                    entityType = (String) data.get("entityType");
                }
            }

            if ("RESUME".equalsIgnoreCase(entityType)) {
                embeddingRepository.saveResumeVector(event.referenceId(), embedding, event.status(), event.errorMessage());
            } else {
                embeddingRepository.saveJobVector(event.referenceId(), embedding, event.status(), event.errorMessage());
            }
        } catch (Exception e) {
            log.error("Error processing AiResultEvent for VECTOR_GEN referenceId: {}", event.referenceId(), e);
        }
    }
}
