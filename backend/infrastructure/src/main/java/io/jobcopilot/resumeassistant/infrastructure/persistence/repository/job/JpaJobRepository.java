package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.job;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaJobRepository extends JpaRepository<JobJpaEntity, String> {

    /**
     * 根据用户 ID 获取职位 JPA 实体列表
     * Get list of Job JPA entities by user ID
     *
     * @param userId 用户 ID / User ID
     * @return JPA 实体列表 / List of JPA entities
     */
    List<JobJpaEntity> findAllByUserId(String userId);

    /**
     * 根据用户 ID 获取未隐藏的职位 JPA 实体列表
     * Get visible Job JPA entities by user ID.
     *
     * @param userId 用户 ID / User ID
     * @return JPA 实体列表 / List of JPA entities
     */
    List<JobJpaEntity> findAllByUserIdAndHiddenAtIsNull(String userId);

    /**
     * 统计用户的职位数量
     * Count jobs by user ID.
     *
     * @param userId 用户 ID / User ID
     * @return 职位数量 / Number of jobs
     */
    long countByUserId(String userId);
}
