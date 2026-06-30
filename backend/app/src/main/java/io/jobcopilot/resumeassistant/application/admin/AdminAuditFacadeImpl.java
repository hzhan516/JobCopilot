package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.AuditLogResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminAuditFacade;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditFacadeImpl implements AdminAuditFacade {

    private final AuditLogRepository auditLogRepository;

    @Override
    public PageResult<AuditLogResponse> listAuditLogs(UUID adminUserId, String action, int page, int size) {
        var result = adminUserId != null
                ? auditLogRepository.findByAdminUserId(adminUserId, page, size)
                : action != null
                        ? auditLogRepository.findByAction(action, page, size)
                        : auditLogRepository.findAll(page, size);
        return PageResult.of(
                result.content().stream().map(this::toResponse).toList(),
                page, size, result.totalElements());
    }

    private AuditLogResponse toResponse(io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId()).adminUserId(log.getAdminUserId())
                .action(log.getAction()).targetType(log.getTargetType())
                .targetId(log.getTargetId()).details(log.getDetails())
                .ipAddress(log.getIpAddress()).createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : null)
                .build();
    }
}
