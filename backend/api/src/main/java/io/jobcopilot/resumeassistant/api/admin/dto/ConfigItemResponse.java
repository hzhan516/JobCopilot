package io.jobcopilot.resumeassistant.api.admin.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ConfigItemResponse(
    String key, String value, String defaultValue,
    String description, String category, String valueType,
    boolean sensitive, boolean readOnly,
    String updatedBy, LocalDateTime updatedAt
) {}
