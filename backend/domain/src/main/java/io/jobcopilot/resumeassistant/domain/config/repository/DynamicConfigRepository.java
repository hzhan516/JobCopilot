package io.jobcopilot.resumeassistant.domain.config.repository;

import io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig;

import java.util.List;
import java.util.Optional;

/** 动态配置仓储端口 / Dynamic config repository port */
public interface DynamicConfigRepository {
    List<DynamicConfig> findAll();
    Optional<DynamicConfig> findByKey(String key);
    DynamicConfig save(DynamicConfig config);
}
