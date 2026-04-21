package edu.asu.ser594.resumeassistant.domain.embedding.repository;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;

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
     * 根据简历版本ID查找向量
     * Find vector by resume version ID
     */
    Optional<ResumeVector> findByResumeVersionId(String resumeVersionId);
}