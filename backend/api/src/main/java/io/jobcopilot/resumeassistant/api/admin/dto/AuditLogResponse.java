package io.jobcopilot.resumeassistant.api.admin.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuditLogResponse(
    UUID id, UUID adminUserId, String action,
    String targetType, String targetId, String details,
    String ipAddress, String createdAt
) {}
