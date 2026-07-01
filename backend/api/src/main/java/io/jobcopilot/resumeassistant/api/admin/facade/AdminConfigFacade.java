package io.jobcopilot.resumeassistant.api.admin.facade;

import io.jobcopilot.resumeassistant.api.admin.dto.ConfigItemResponse;

import java.util.List;
import java.util.UUID;

public interface AdminConfigFacade {
    List<ConfigItemResponse> getAllConfigs();
    ConfigItemResponse getConfig(String key);
    ConfigItemResponse updateConfig(String key, String value, UUID adminUserId);
    ConfigItemResponse resetConfig(String key, UUID adminUserId);
}
