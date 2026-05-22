package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.tracking;

import io.jobcopilot.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.tracking.ApplicationTrackingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 求职申请跟踪 Spring Data JPA 接口
 * Application tracking Spring Data JPA repository
 */
@Repository
public interface JpaApplicationTrackingRepository extends JpaRepository<ApplicationTrackingJpaEntity, String> {

    Optional<ApplicationTrackingJpaEntity> findByIdAndUserId(String id, String userId);

    List<ApplicationTrackingJpaEntity> findAllByUserId(String userId);

    List<ApplicationTrackingJpaEntity> findAllByUserIdAndStatus(String userId, ApplicationStatus status);
}
