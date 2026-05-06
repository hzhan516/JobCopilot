package edu.asu.ser594.resumeassistant.application.embedding.service;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 失败向量持久化服务 / Failed vector persistence service
 * <p>
 * 负责在独立事务中保存向量生成失败的记录，确保失败记录不会因外层事务回滚而丢失。
 * Responsible for persisting failed vector generation records in an independent transaction,
 * ensuring failure records survive outer transaction rollbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedVectorPersistenceService {

    private final ResumeVectorRepository resumeVectorRepository;
    private final JobVectorRepository jobVectorRepository;

    /**
     * 保存失败的向量记录
     * Save a failed vector record.
     *
     * <p>该方法始终在一个全新事务中执行（REQUIRES_NEW），
     * 因此即使调用方的事务已标记为 rollback-only，失败记录仍可成功提交。
     * <p>This method always runs in a brand-new transaction (REQUIRES_NEW),
     * so the failure record commits successfully even when the caller's transaction
     * has been marked rollback-only.
     *
     * @param referenceId  实体 ID / Entity ID
     * @param entityType   实体类型 ("JOB" or "RESUME") / Entity type
     * @param errorMessage 错误信息 / Error message
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedVector(String referenceId, String entityType, String errorMessage) {
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
