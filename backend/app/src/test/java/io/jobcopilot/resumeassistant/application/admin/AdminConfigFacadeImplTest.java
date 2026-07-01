package io.jobcopilot.resumeassistant.application.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.api.admin.dto.ConfigItemResponse;
import io.jobcopilot.resumeassistant.domain.admin.repository.AuditLogRepository;
import io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig;
import io.jobcopilot.resumeassistant.domain.config.repository.DynamicConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminConfigFacadeImpl 单元测试 / Unit tests for AdminConfigFacadeImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Config Facade Tests")
class AdminConfigFacadeImplTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private DynamicConfigRepository configRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminConfigFacadeImpl facade;

    private DynamicConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = DynamicConfig.builder()
                .configKey("log.aiServiceLevel")
                .configValue("INFO")
                .defaultValue("INFO")
                .description("AI service log level")
                .category("logging")
                .valueType("STRING")
                .sensitive(false)
                .readOnly(false)
                .build();
        when(auditLogRepository.save(any())).thenReturn(null);
    }

    @Test
    @DisplayName("Should update config and publish change event")
    void shouldUpdateConfigAndPublishChangeEvent() {
        when(configRepository.findByKey("log.aiServiceLevel")).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any())).thenReturn(testConfig);
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        ConfigItemResponse response = facade.updateConfig("log.aiServiceLevel", "DEBUG", ADMIN_ID);

        assertThat(response.getValue()).isEqualTo("DEBUG");
        verify(configRepository).save(testConfig);
        verify(auditLogRepository).save(any());
        verify(stringRedisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());
        assertThat(channelCaptor.getValue()).isEqualTo("config:changed");
        assertThat(messageCaptor.getValue()).contains("\"key\":\"log.aiServiceLevel\"");
        assertThat(messageCaptor.getValue()).contains("\"value\":\"DEBUG\"");
    }

    @Test
    @DisplayName("Should throw when updating read-only config")
    void shouldThrowWhenUpdatingReadOnlyConfig() {
        DynamicConfig readOnly = DynamicConfig.builder()
                .configKey("readonly.key")
                .configValue("v1")
                .defaultValue("v1")
                .description("Read only")
                .category("system")
                .valueType("STRING")
                .sensitive(false)
                .readOnly(true)
                .build();
        when(configRepository.findByKey("readonly.key")).thenReturn(Optional.of(readOnly));

        assertThatThrownBy(() -> facade.updateConfig("readonly.key", "v2", ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-only");

        verify(stringRedisTemplate, never()).convertAndSend(any(), any());
    }

    @Test
    @DisplayName("Should throw when config not found")
    void shouldThrowWhenConfigNotFound() {
        when(configRepository.findByKey("missing.key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.updateConfig("missing.key", "x", ADMIN_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Config not found");

        verify(stringRedisTemplate, never()).convertAndSend(any(), any());
    }

    @Test
    @DisplayName("Should reset config and publish change event")
    void shouldResetConfigAndPublishChangeEvent() {
        when(configRepository.findByKey("log.aiServiceLevel")).thenReturn(Optional.of(testConfig));
        when(configRepository.save(any())).thenReturn(testConfig);

        facade.resetConfig("log.aiServiceLevel", ADMIN_ID);

        verify(configRepository).save(testConfig);
        verify(stringRedisTemplate).convertAndSend(any(), any());
    }
}
