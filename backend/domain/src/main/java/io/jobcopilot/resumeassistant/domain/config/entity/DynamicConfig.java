package io.jobcopilot.resumeassistant.domain.config.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/** 动态配置项 / Dynamic configuration entry */
@Getter
@Builder
public class DynamicConfig {
    private final String configKey;
    private String configValue;
    private final String defaultValue;
    private final String description;
    private final String category;
    private final String valueType;
    private final boolean sensitive;
    private final boolean readOnly;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private final LocalDateTime createdAt;

    public void update(String value, UUID by) {
        this.configValue = value;
        this.updatedBy = by;
        this.updatedAt = LocalDateTime.now();
    }
}
