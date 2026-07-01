package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.config;

import io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dynamic_config")
@Getter @Setter @NoArgsConstructor
public class DynamicConfigJpaEntity {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "jsonb", nullable = false)
    private String configValue;

    @Column(name = "default_value", columnDefinition = "jsonb", nullable = false)
    private String defaultValue;

    @Column(length = 500)
    private String description;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(name = "value_type", length = 20, nullable = false)
    private String valueType = "STRING";

    @Column(name = "is_sensitive")
    private boolean sensitive;

    @Column(name = "is_readonly")
    private boolean readOnly;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DynamicConfig toDomain() {
        return DynamicConfig.builder()
                .configKey(configKey).configValue(configValue).defaultValue(defaultValue)
                .description(description).category(category).valueType(valueType)
                .sensitive(sensitive).readOnly(readOnly)
                .updatedBy(updatedBy).updatedAt(updatedAt).createdAt(createdAt)
                .build();
    }

    public static DynamicConfigJpaEntity fromDomain(DynamicConfig d) {
        var e = new DynamicConfigJpaEntity();
        e.configKey = d.getConfigKey();
        e.configValue = d.getConfigValue();
        e.defaultValue = d.getDefaultValue();
        e.description = d.getDescription();
        e.category = d.getCategory();
        e.valueType = d.getValueType();
        e.sensitive = d.isSensitive();
        e.readOnly = d.isReadOnly();
        e.updatedBy = d.getUpdatedBy();
        e.updatedAt = d.getUpdatedAt();
        e.createdAt = d.getCreatedAt();
        return e;
    }
}
