package io.jobcopilot.resumeassistant.infrastructure.cache.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig;
import io.jobcopilot.resumeassistant.domain.config.repository.DynamicConfigRepository;
import io.jobcopilot.resumeassistant.infrastructure.cache.config.DynamicConfigCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * DynamicConfigListener 单元测试 / Unit tests for DynamicConfigListener.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dynamic Config Listener Tests")
class DynamicConfigListenerTest {

    @Mock
    private DynamicConfigRepository dynamicConfigRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DynamicConfigListener listener;

    @BeforeEach
    void setUp() {
        listener = new DynamicConfigListener(dynamicConfigRepository, objectMapper);
        DynamicConfigCache.clear();
    }

    @Test
    @DisplayName("Should update cache when config exists")
    void shouldUpdateCacheWhenConfigExists() {
        DynamicConfig config = DynamicConfig.builder()
                .configKey("log.aiServiceLevel")
                .configValue("DEBUG")
                .defaultValue("INFO")
                .description("Log level")
                .category("logging")
                .valueType("STRING")
                .sensitive(false)
                .readOnly(false)
                .build();
        when(dynamicConfigRepository.findByKey("log.aiServiceLevel")).thenReturn(Optional.of(config));

        listener.onMessage(messageOf("{\"key\": \"log.aiServiceLevel\"}"), null);

        assertThat(DynamicConfigCache.get("log.aiServiceLevel")).isEqualTo("DEBUG");
        verify(dynamicConfigRepository).findByKey("log.aiServiceLevel");
    }

    @Test
    @DisplayName("Should mask sensitive config value in cache")
    void shouldMaskSensitiveConfigValueInCache() {
        DynamicConfig config = DynamicConfig.builder()
                .configKey("secret.key")
                .configValue("top-secret")
                .defaultValue("default")
                .description("Secret")
                .category("security")
                .valueType("STRING")
                .sensitive(true)
                .readOnly(false)
                .build();
        when(dynamicConfigRepository.findByKey("secret.key")).thenReturn(Optional.of(config));

        listener.onMessage(messageOf("{\"key\": \"secret.key\"}"), null);

        assertThat(DynamicConfigCache.get("secret.key")).isEqualTo("***");
    }

    @Test
    @DisplayName("Should ignore message when key not found")
    void shouldIgnoreMessageWhenKeyNotFound() {
        when(dynamicConfigRepository.findByKey("unknown.key")).thenReturn(Optional.empty());

        listener.onMessage(messageOf("{\"key\": \"unknown.key\"}"), null);

        assertThat(DynamicConfigCache.get("unknown.key")).isNull();
    }

    @Test
    @DisplayName("Should ignore invalid JSON message")
    void shouldIgnoreInvalidJsonMessage() {
        listener.onMessage(messageOf("not-json"), null);

        verifyNoInteractions(dynamicConfigRepository);
    }

    @Test
    @DisplayName("Should ignore message with blank key")
    void shouldIgnoreMessageWithBlankKey() {
        listener.onMessage(messageOf("{\"key\": \" \"}"), null);

        verifyNoInteractions(dynamicConfigRepository);
    }

    private Message messageOf(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return new Message() {
            @Override
            public byte[] getBody() {
                return bytes;
            }

            @Override
            public byte[] getChannel() {
                return "config:changed".getBytes(StandardCharsets.UTF_8);
            }
        };
    }
}
