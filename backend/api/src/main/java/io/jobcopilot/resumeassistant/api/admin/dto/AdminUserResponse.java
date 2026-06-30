package io.jobcopilot.resumeassistant.api.admin.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminUserResponse(
    UUID id, String email, String role, String status,
    String authProvider, boolean emailVerified,
    long resumeCount, long jobCount, long conversationCount,
    String createdAt, String updatedAt
) {}
