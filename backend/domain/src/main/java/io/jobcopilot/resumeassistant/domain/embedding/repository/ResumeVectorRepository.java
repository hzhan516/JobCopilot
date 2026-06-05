package io.jobcopilot.resumeassistant.domain.embedding.repository;

import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;

import java.util.List;
import java.util.Optional;

/**
 * 简历向量仓储接口
 * Resume vector repository interface
 */
public interface ResumeVectorRepository {

    /**
     * 保存简历向量
     * Save resume vector
     */
    void save(ResumeVector vector);

    /**
     * 批量保存简历向量
     * Save resume vectors in batch
     */
    void saveAll(List<ResumeVector> vectors);

    /**
     * 根据简历版本ID查找向量
     * Find vector by resume version ID
     */
    Optional<ResumeVector> findByResumeVersionId(String resumeVersionId);
}
