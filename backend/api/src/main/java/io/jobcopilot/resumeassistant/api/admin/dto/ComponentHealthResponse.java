package io.jobcopilot.resumeassistant.api.admin.dto;

import lombok.Builder;

@Builder
public record ComponentHealthResponse(
    boolean postgres, boolean redis, boolean rabbitmq,
    boolean aiService, boolean minio
) {}
