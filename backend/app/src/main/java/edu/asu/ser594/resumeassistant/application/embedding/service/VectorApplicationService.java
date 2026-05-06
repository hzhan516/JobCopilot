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
 * 向量应用服务 / Vector application service
 * <p>
 * 负责调用 AI Service REST 端点同步生成嵌入向量并持久化到领域仓库。
 * 所有写操作均在事务边界内执行，遵循 CQRS 原则。
 * Responsible for calling AI Service REST endpoint to synchronously generate embeddings
 * and persist them to domain repositories.
 * All write operations are executed within transaction boundaries, following CQRS.
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
     * 同步生成向量并保存
     * Synchronously generate embedding and persist.
     *
     * @param referenceId 实体 ID / Entity ID
     * @param entityType  实体类型 ("JOB" or "RESUME") / Entity type
     * @param text        待嵌入文本 / Text to embed
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

    // 保存成功的向量 / Save completed vector
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

    // 保存失败的向量 / Save failed vector
    private void saveFailedVector(String referenceId, String entityType, String errorMessage) {
        failedVectorPersistenceService.saveFailedVector(referenceId, entityType, errorMessage);
    }

    private boolean isResumeEntity(String entityType) {
        return "RESUME".equalsIgnoreCase(entityType) || "RESUME_VECTOR".equalsIgnoreCase(entityType);
    }
}
