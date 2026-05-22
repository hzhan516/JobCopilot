package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.matching;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.matching.JobMatchResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 职位匹配结果 Spring Data JPA 接口
 * Job match result Spring Data JPA repository
 */
@Repository
public interface JpaJobMatchResultRepository extends JpaRepository<JobMatchResultJpaEntity, String> {

    List<JobMatchResultJpaEntity> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
