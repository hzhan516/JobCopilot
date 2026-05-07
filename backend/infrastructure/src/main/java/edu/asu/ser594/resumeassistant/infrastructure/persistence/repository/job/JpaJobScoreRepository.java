package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.job;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job.JobScoreRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaJobScoreRepository extends JpaRepository<JobScoreRecordJpaEntity, String> {

    /**
     * 根据职位 ID 查询所有评分记录，按 created_at 倒序
     * Find all score records by job ID ordered by created_at desc.
     */
    List<JobScoreRecordJpaEntity> findAllByJobIdOrderByCreatedAtDesc(String jobId);

    /**
     * 根据用户 ID 查询所有评分记录，按 created_at 倒序
     * Find all score records by user ID ordered by created_at desc.
     */
    List<JobScoreRecordJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据职位 ID 和简历版本 ID 查询所有评分记录，按 created_at 倒序
     * Find all score records by job ID and resume version ID ordered by created_at desc.
     */
    List<JobScoreRecordJpaEntity> findAllByJobIdAndResumeVersionIdOrderByCreatedAtDesc(String jobId, String resumeVersionId);
}
