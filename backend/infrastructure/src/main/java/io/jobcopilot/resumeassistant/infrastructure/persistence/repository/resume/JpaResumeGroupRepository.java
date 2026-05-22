package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.resume;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历组Spring Data JPA仓库
 */
@Repository
public interface JpaResumeGroupRepository extends JpaRepository<ResumeGroupJpaEntity, UUID> {

    List<ResumeGroupJpaEntity> findAllByUserId(UUID userId);

    Optional<ResumeGroupJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ResumeGroupJpaEntity> findByUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE ResumeGroupJpaEntity g SET g.isDefault = false WHERE g.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
