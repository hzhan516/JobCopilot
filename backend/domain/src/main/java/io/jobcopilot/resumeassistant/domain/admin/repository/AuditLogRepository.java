package io.jobcopilot.resumeassistant.domain.admin.repository;

import io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog;
import io.jobcopilot.resumeassistant.types.common.PageResult;

import java.util.Optional;
import java.util.UUID;

/** 审计日志仓储端口 / Audit log repository port */
public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    Optional<AuditLog> findById(UUID id);
    PageResult<AuditLog> findAll(int page, int size);
    PageResult<AuditLog> findByAdminUserId(UUID adminUserId, int page, int size);
    PageResult<AuditLog> findByAction(String action, int page, int size);
}
