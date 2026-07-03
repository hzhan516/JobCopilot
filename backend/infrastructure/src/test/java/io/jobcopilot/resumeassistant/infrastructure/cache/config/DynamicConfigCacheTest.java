package io.jobcopilot.resumeassistant.infrastructure.cache.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DynamicConfigCache 单元测试 / Unit tests for DynamicConfigCache.
 */
@DisplayName("Dynamic Config Cache Tests")
class DynamicConfigCacheTest {

    @BeforeEach
    void setUp() {
        DynamicConfigCache.clear();
    }

    @Test
    @DisplayName("Should store and retrieve value")
    void shouldStoreAndRetrieveValue() {
        DynamicConfigCache.put("app.name", "JobCopilot");

        assertThat(DynamicConfigCache.get("app.name")).isEqualTo("JobCopilot");
    }

    @Test
    @DisplayName("Should return null for missing key")
    void shouldReturnNullForMissingKey() {
        assertThat(DynamicConfigCache.get("missing.key")).isNull();
    }

    @Test
    @DisplayName("Should update existing value")
    void shouldUpdateExistingValue() {
        DynamicConfigCache.put("app.name", "Old");
        DynamicConfigCache.put("app.name", "New");

        assertThat(DynamicConfigCache.get("app.name")).isEqualTo("New");
    }

    @Test
    @DisplayName("Should clear all entries")
    void shouldClearAllEntries() {
        DynamicConfigCache.put("app.name", "JobCopilot");
        DynamicConfigCache.put("app.version", "1.0.0");

        DynamicConfigCache.clear();

        assertThat(DynamicConfigCache.get("app.name")).isNull();
        assertThat(DynamicConfigCache.get("app.version")).isNull();
    }
}
