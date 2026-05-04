package edu.asu.ser594.resumeassistant.domain.job.repository;

import edu.asu.ser594.resumeassistant.domain.job.entity.JobScoreRecord;

import java.util.List;
import java.util.Optional;

/**
 * 职位评分记录仓储接口
 * Repository interface for job score records.
 */
public interface JobScoreRepository {

    /**
     * 保存评分记录
     * Saves a job score record.
     *
     * @param record 评分记录 / The score record to save.
     * @return 保存后的记录 / The saved record.
     */
    JobScoreRecord save(JobScoreRecord record);

    /**
     * 根据职位 ID 查询所有评分记录（按时间倒序）
     * Finds all score records for a job (ordered by createdAt desc).
     *
     * @param jobId 职位 ID / The job ID.
     * @return 评分记录列表 / List of score records.
     */
    List<JobScoreRecord> findAllByJobIdOrderByCreatedAtDesc(String jobId);

    /**
     * 根据职位 ID 和简历版本 ID 查询最新评分记录
     * Finds the latest score record for a specific job and resume version.
     *
     * @param jobId           职位 ID / The job ID.
     * @param resumeVersionId 简历版本 ID / The resume version ID.
     * @return 最新评分记录(可选) / The latest score record, if any.
     */
    Optional<JobScoreRecord> findLatestByJobIdAndResumeVersionId(String jobId, String resumeVersionId);
}
