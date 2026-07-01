package io.jobcopilot.resumeassistant.domain.admin.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/** 管理员审计日志 / Admin audit log */
@Getter
@Builder
public class AuditLog {
    private final UUID id;
    private final UUID adminUserId;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String details;
    private final String ipAddress;
    private final LocalDateTime createdAt;
}
