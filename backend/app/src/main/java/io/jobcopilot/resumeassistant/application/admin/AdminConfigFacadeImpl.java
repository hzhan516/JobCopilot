package io.jobcopilot.resumeassistant.application.admin;

import io.jobcopilot.resumeassistant.api.admin.dto.ConfigItemResponse;
import io.jobcopilot.resumeassistant.api.admin.facade.AdminConfigFacade;
import io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.config.repository.DynamicConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class AdminConfigFacadeImpl implements AdminConfigFacade {

    private final DynamicConfigRepository configRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConfigItemResponse> getAllConfigs() {
        return configRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigItemResponse getConfig(String key) {
        var cfg = configRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
        return toResponse(cfg);
    }

    @Override
    public ConfigItemResponse updateConfig(String key, String value, UUID adminUserId) {
        var cfg = configRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
        if (cfg.isReadOnly()) throw new IllegalArgumentException("Config is read-only: " + key);
        cfg.update(value, adminUserId);
        var saved = configRepository.save(cfg);
        audit(adminUserId, "UPDATE_CONFIG", "config", key, value);
        return toResponse(saved);
    }

    @Override
    public ConfigItemResponse resetConfig(String key, UUID adminUserId) {
        var cfg = configRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
        cfg.update(cfg.getDefaultValue(), adminUserId);
        var saved = configRepository.save(cfg);
        audit(adminUserId, "RESET_CONFIG", "config", key, cfg.getDefaultValue());
        return toResponse(saved);
    }

    private ConfigItemResponse toResponse(io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig c) {
        return ConfigItemResponse.builder()
                .key(c.getConfigKey())
                .value(c.isSensitive() ? "***" : c.getConfigValue())
                .defaultValue(c.isSensitive() ? "***" : c.getDefaultValue())
                .description(c.getDescription()).category(c.getCategory())
                .valueType(c.getValueType()).sensitive(c.isSensitive()).readOnly(c.isReadOnly())
                .updatedBy(c.getUpdatedBy() != null ? c.getUpdatedBy().toString() : null)
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private void audit(UUID adminUserId, String action, String targetType, String targetId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .id(UUID.randomUUID()).adminUserId(adminUserId)
                .action(action).targetType(targetType).targetId(targetId)
                .details(details).createdAt(LocalDateTime.now()).build());
    }
}
