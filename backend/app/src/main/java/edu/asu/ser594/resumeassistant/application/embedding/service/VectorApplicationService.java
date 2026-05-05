package edu.asu.ser594.resumeassistant.application.embedding.service;

import edu.asu.ser594.resumeassistant.api.embedding.config.EmbeddingConfig;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 向量应用服务 / Vector application service
 * <p>
 * 负责解析 AI 返回的向量生成结果并持久化到领域仓库。
 * 所有写操作均在事务边界内执行，遵循 CQRS 原则。
 * All write operations are executed within transaction boundaries, following CQRS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorApplicationService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingConfig embeddingConfig;

    /**
     * 处理向量生成结果并保存
     * Process vector generation result and persist.
     *
     * @param event AI 结果事件 / AI result event
     */
    @Transactional
    public void handleVectorGenResult(AiResultEvent event) {
        String entityType = event.eventType() != null ? event.eventType() : extractEntityType(event);
        float[] embeddingArray = extractEmbedding(event);

        if ("COMPLETED".equals(event.status()) && embeddingArray != null) {
            if (embeddingArray.length != embeddingConfig.getDimension()) {
                log.error("向量维度不匹配: 期望 {}, 实际 {}。referenceId: {}, entityType: {}",
                        embeddingConfig.getDimension(), embeddingArray.length,
                        event.referenceId(), entityType);
                saveFailedVector(event.referenceId(), entityType,
                        "Embedding dimension mismatch: expected " + embeddingConfig.getDimension()
                                + ", got " + embeddingArray.length);
                return;
            }
            saveCompletedVector(event.referenceId(), entityType, embeddingArray);
        } else {
            saveFailedVector(event.referenceId(), entityType, event.errorMessage());
        }
    }

    // 提取实体类型 / Extract entity type
    private String extractEntityType(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("entityType")) {
            return (String) event.data().get("entityType");
        }
        return "JOB";
    }

    // 提取嵌入向量 / Extract embedding vector
    private float[] extractEmbedding(AiResultEvent event) {
        if (event.data() != null && event.data().containsKey("embedding")) {
            Object rawEmbedding = event.data().get("embedding");
            if (rawEmbedding instanceof List<?> list) {
                float[] arr = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object val = list.get(i);
                    if (val instanceof Number n) {
                        arr[i] = n.floatValue();
                    }
                }
                return arr;
            }
        }
        return null;
    }

    // 保存成功的向量 / Save completed vector
    private void saveCompletedVector(String referenceId, String entityType, float[] embedding) {
        String id = UUID.randomUUID().toString();
        if (isResumeEntity(entityType)) {
            ResumeVector vector = ResumeVector.createCompleted(id, referenceId, embedding);
            resumeVectorRepository.save(vector);
            log.info("Saved COMPLETED resume vector for versionId: {}", referenceId);
        } else {
            JobVector vector = JobVector.createCompleted(id, referenceId, embedding);
            jobVectorRepository.save(vector);
            log.info("Saved COMPLETED job vector for jobId: {}", referenceId);
        }
    }

    // 保存失败的向量 / Save failed vector
    private void saveFailedVector(String referenceId, String entityType, String errorMessage) {
        String id = UUID.randomUUID().toString();
        if (isResumeEntity(entityType)) {
            ResumeVector vector = ResumeVector.createFailed(id, referenceId, errorMessage);
            resumeVectorRepository.save(vector);
            log.warn("Saved FAILED resume vector for versionId: {}", referenceId);
        } else {
            JobVector vector = JobVector.createFailed(id, referenceId, errorMessage);
            jobVectorRepository.save(vector);
            log.warn("Saved FAILED job vector for jobId: {}", referenceId);
        }
    }

    private boolean isResumeEntity(String entityType) {
        return "RESUME".equalsIgnoreCase(entityType) || "RESUME_VECTOR".equalsIgnoreCase(entityType);
    }
}
