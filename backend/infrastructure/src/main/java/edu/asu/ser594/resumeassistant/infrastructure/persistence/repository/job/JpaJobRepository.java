package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.job;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaJobRepository extends JpaRepository<JobJpaEntity, String> {

    /**
     * 根据用户 ID 获取职位 JPA 实体列表
     * Get list of Job JPA entities by user ID
     *
     * @param userId 用户 ID / User ID
     * @return JPA 实体列表 / List of JPA entities
     */
    List<JobJpaEntity> findAllByUserId(UUID userId);
}
