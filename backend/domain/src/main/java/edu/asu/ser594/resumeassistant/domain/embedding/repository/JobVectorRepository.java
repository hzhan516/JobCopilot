package edu.asu.ser594.resumeassistant.domain.embedding.repository;

import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;

import java.util.Optional;

/**
 * 职位向量仓储接口
 * Job vector repository interface
 */
public interface JobVectorRepository {

    /**
     * 保存职位向量
     * Save job vector
     */
    void save(JobVector vector);

    /**
     * 根据职位ID查找向量
     * Find vector by job ID
     */
    Optional<JobVector> findByJobId(String jobId);
}