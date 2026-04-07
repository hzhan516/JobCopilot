package edu.asu.ser594.resumeassistant.infrastructure.repository.resume;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 简历JPA仓储
 * Resume JPA repository
 */
@Repository
public interface JpaResumeRepository extends JpaRepository<ResumeJpaEntity, UUID> {

    Optional<ResumeJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<ResumeJpaEntity> findByUserId(UUID userId, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
