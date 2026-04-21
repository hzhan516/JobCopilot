package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.matching;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.matching.JobDatasetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 职位数据集 Spring Data JPA 接口
 * Job dataset Spring Data JPA repository
 */
@Repository
public interface JpaJobDatasetRepository extends JpaRepository<JobDatasetJpaEntity, Long> {

    Optional<JobDatasetJpaEntity> findByExternalId(String externalId);
}
