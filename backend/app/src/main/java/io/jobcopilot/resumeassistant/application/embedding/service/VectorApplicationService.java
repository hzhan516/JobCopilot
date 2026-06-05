package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.config.EmbeddingConfig;
import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorEmbeddingPort;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Synchronous embedding gateway that bridges domain write operations with the AI service.
 * The expensive HTTP call to the AI service happens outside any explicit transaction,
 * while the lightweight DB write is handled by the repository's implicit short transaction.
 * 同步嵌入网关，衔接领域写操作与 AI 服务。耗时的 HTTP 调用发生在显式事务外，轻量的 DB 写入由仓库的隐式短事务处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorApplicationService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingConfig embeddingConfig;
    private final VectorEmbeddingPort vectorEmbeddingPort;
    private final FailedVectorPersistenceService failedVectorPersistenceService;

    /**
     * Validates dimension alignment, then persists the result. Dimension mismatches are treated as hard failures
     * to prevent silent corruption of the vector index.
     * 根据配置的模型校验维度一致性，然后持久化结果。维度不匹配被视为硬失败，以防止向量索引被静默污染
     *
     * @param referenceId Entity ID / 实体 ID
     * @param entityType  Entity type ("JOB" or "RESUME") / 实体类型
     * @param text        Text to embed / 待嵌入文本
     */
    public void generateAndSaveVector(String referenceId, String entityType, String text) {
        try {
            float[] embedding = vectorEmbeddingPort.generate(text);

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
