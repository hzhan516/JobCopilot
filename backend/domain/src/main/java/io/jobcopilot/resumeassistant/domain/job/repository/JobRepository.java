package io.jobcopilot.resumeassistant.domain.job.repository;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 职位聚合根仓储接口
 * Repository interface for Job aggregates.
 */
public interface JobRepository {

    /**
     * 保存职位实体
     * Saves a job to the repository.
     *
     * @param job 职位聚合根 / The job to save.
     */
    void save(Job job);

    /**
     * 根据ID查询职位
     * Finds a job by its unique identifier.
     *
     * @param id 职位ID / The ID of the job.
     * @return 职位实体(可选) / An Optional containing the job if found, or empty otherwise.
     */
    Optional<Job> findById(String id);

    /**
     * 根据用户 ID 获取职位列表
     * Get job list by user ID
     *
     * @param userId 用户 ID / User ID
     * @return 职位实体列表 / List of job entities
     */
    List<Job> findAllByUserId(UUID userId);

    /**
     * 统计所有职位数量
     * Count all jobs
     *
     * @return 职位数量 / Number of jobs
     */
    long count();
}

