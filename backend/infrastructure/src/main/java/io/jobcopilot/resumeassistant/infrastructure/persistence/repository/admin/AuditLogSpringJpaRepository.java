package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.admin;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.admin.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AuditLogSpringJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    Page<AuditLogJpaEntity> findByAdminUserId(UUID adminUserId, Pageable pageable);
    Page<AuditLogJpaEntity> findByAction(String action, Pageable pageable);
    long countByActionContaining(String actionSubstring);
}
