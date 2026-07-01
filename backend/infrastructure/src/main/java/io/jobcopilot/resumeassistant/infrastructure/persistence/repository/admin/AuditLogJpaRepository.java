package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.admin;

import io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.admin.AuditLogJpaEntity;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditLogJpaRepository implements AuditLogRepository {

    private final AuditLogSpringJpaRepository springRepo;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return springRepo.save(AuditLogJpaEntity.fromDomain(auditLog)).toDomain();
    }

    @Override
    public Optional<AuditLog> findById(UUID id) {
        return springRepo.findById(id).map(AuditLogJpaEntity::toDomain);
    }

    @Override
    public PageResult<AuditLog> findAll(int page, int size) {
        var p = springRepo.findAll(PageRequest.of(page, size));
        return PageResult.of(p.map(AuditLogJpaEntity::toDomain).getContent(), page, size, p.getTotalElements());
    }

    @Override
    public PageResult<AuditLog> findByAdminUserId(UUID adminUserId, int page, int size) {
        var p = springRepo.findByAdminUserId(adminUserId, PageRequest.of(page, size));
        return PageResult.of(p.map(AuditLogJpaEntity::toDomain).getContent(), page, size, p.getTotalElements());
    }

    @Override
    public PageResult<AuditLog> findByAction(String action, int page, int size) {
        var p = springRepo.findByAction(action, PageRequest.of(page, size));
        return PageResult.of(p.map(AuditLogJpaEntity::toDomain).getContent(), page, size, p.getTotalElements());
    }
}
