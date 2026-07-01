package io.jobcopilot.resumeassistant.api.admin.facade;

import io.jobcopilot.resumeassistant.api.admin.dto.AuditLogResponse;
import io.jobcopilot.resumeassistant.types.common.PageResult;

import java.util.UUID;

public interface AdminAuditFacade {
    PageResult<AuditLogResponse> listAuditLogs(UUID adminUserId, String action, int page, int size);
}
