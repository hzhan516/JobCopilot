package edu.asu.ser594.resumeassistant.application.embedding.service;

import edu.asu.ser594.resumeassistant.api.embedding.config.EmbeddingConfig;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Synchronous embedding gateway that bridges domain write operations with the AI service.
 * Every vector write is wrapped in a transaction so failures can be recorded as FAILED rows
 * instead of silently dropping the request.
 * 同步嵌入网关，衔接领域写操作与 AI 服务。每次向量写入均包裹在事务中，使失败可被记录为 FAILED 行而非静默丢弃请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorApplicationService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingConfig embeddingConfig;
    private final EmbeddingService embeddingService;
    private final FailedVectorPersistenceService failedVectorPersistenceService;

    /**
     * Generates an embedding for the given text, validates dimension alignment against the
     * configured model, and persists the result. Dimension mismatches are treated as hard failures
     * to prevent silent corruption of the vector index.
     * 为给定文本生成嵌入向量，根据配置的模型校验维度一致性，然后持久化结果。维度不匹配被视为硬失败，以防止向量索引被静默污染
     *
     * @param referenceId Entity ID / 实体 ID
     * @param entityType  Entity type ("JOB" or "RESUME") / 实体类型
     * @param text        Text to embed / 待嵌入文本
     */
    @Transactional
    public void generateAndSaveVector(String referenceId, String entityType, String text) {
        try {
            float[] embedding = embeddingService.generate(text);

            if (embedding.length != embeddingConfig.getDimension()) {
                log.error("向量维度不匹配: 期望 {}, 实际 {}。referenceId: {}, entityType: {}",
                        embeddingConfig.getDimension(), embedding.length,
                        referenceId, entityType);
                saveFailedVector(referenceId, entityType,
                        "Embedding dimension mismatch: expected " + embeddingConfig.getDimension()
                                + ", got " + embedding.length);
                return;
            }

            saveCompletedVector(referenceId, entityType, embedding);
            log.info("Vector generated and saved for {}: {}", entityType, referenceId);

        } catch (Exception e) {
            log.error("向量生成失败: referenceId={}, entityType={}", referenceId, entityType, e);
            saveFailedVector(referenceId, entityType, e.getMessage());
        }
    }

    private void saveCompletedVector(String referenceId, String entityType, float[] embedding) {
        String id = UUID.randomUUID().toString();
        if (isResumeEntity(entityType)) {
            ResumeVector vector = ResumeVector.createCompleted(id, referenceId, embedding);
            resumeVectorRepository.save(vector);
            log.info("Saved COMPLETED resume vector for versionId: {}", referenceId);
        } else {
            JobVector vector = JobVector.createCompleted(
                    id, referenceId, embedding,
                    null, null, null, null, null,
                    embeddingConfig.getDefaultModelVersion()
            );
            jobVectorRepository.save(vector);
            log.info("Saved COMPLETED job vector for jobId: {}", referenceId);
        }
    }

    private void saveFailedVector(String referenceId, String entityType, String errorMessage) {
        failedVectorPersistenceService.saveFailedVector(referenceId, entityType, errorMessage);
    }

    private boolean isResumeEntity(String entityType) {
        return "RESUME".equalsIgnoreCase(entityType) || "RESUME_VECTOR".equalsIgnoreCase(entityType);
    }
}
