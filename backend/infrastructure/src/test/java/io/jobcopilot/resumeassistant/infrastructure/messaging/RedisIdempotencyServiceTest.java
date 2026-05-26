package io.jobcopilot.resumeassistant.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Redis 幂等性服务单元测试
 * Redis Idempotency Service Unit Tests
 *
 * 测试消息去重核心逻辑：
 * Tests message deduplication core logic:
 * - 首次处理 / First-time processing
 * - 重复检测 / Duplicate detection
 * - 标记处理完成 / Mark processed
 * - TTL 设置 / TTL configuration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Redis Idempotency Service Tests")
class RedisIdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisIdempotencyService idempotencyService;

    private static final String EVENT_KEY = "event-uuid-123";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 重复检测 ====================
    // ==================== Duplicate Detection ====================

    @Test
    @DisplayName("Should return false for unprocessed event")
    void shouldReturnFalseForUnprocessedEvent() {
        // 给定 / Given
        when(redisTemplate.hasKey("mq:idempotency:" + EVENT_KEY)).thenReturn(false);

        // 当 / When
        boolean processed = idempotencyService.isProcessed(EVENT_KEY);

        // 那么 / Then
        assertThat(processed).isFalse();
    }

    @Test
    @DisplayName("Should return true for already processed event")
    void shouldReturnTrueForAlreadyProcessedEvent() {
        // 给定 / Given
        when(redisTemplate.hasKey("mq:idempotency:" + EVENT_KEY)).thenReturn(true);

        // 当 / When
        boolean processed = idempotencyService.isProcessed(EVENT_KEY);

        // 那么 / Then
        assertThat(processed).isTrue();
    }

    @Test
    @DisplayName("Should handle null hasKey result")
    void shouldHandleNullHasKeyResult() {
        // 给定 / Given
        when(redisTemplate.hasKey(anyString())).thenReturn(null);

        // 当 / When
        boolean processed = idempotencyService.isProcessed(EVENT_KEY);

        // 那么 / Then — Boolean.TRUE.equals(null) returns false
        assertThat(processed).isFalse();
    }

    // ==================== 标记处理 ====================
    // ==================== Mark Processed ====================

    @Test
    @DisplayName("Should mark event as processed with custom TTL")
    void shouldMarkEventAsProcessedWithCustomTtl() {
        // 给定 / Given
        Duration ttl = Duration.ofMinutes(30);

        // 当 / When
        idempotencyService.markProcessed(EVENT_KEY, ttl);

        // 那么 / Then
        verify(valueOperations).set("mq:idempotency:" + EVENT_KEY, "1", ttl);
    }

    @Test
    @DisplayName("Should mark event as processed with default TTL")
    void shouldMarkEventAsProcessedWithDefaultTtl() {
        // 当 / When
        idempotencyService.markProcessed(EVENT_KEY);

        // 那么 / Then
        verify(valueOperations).set(
                eq("mq:idempotency:" + EVENT_KEY),
                eq("1"),
                argThat((Duration d) -> d.getSeconds() == Duration.ofHours(24).getSeconds())
        );
    }

    @Test
    @DisplayName("Should use correct key prefix")
    void shouldUseCorrectKeyPrefix() {
        // 当 / When
        idempotencyService.isProcessed("my-event");

        // 那么 / Then
        verify(redisTemplate).hasKey("mq:idempotency:my-event");
    }

    @Test
    @DisplayName("Should handle empty event key")
    void shouldHandleEmptyEventKey() {
        // 当 / When
        idempotencyService.markProcessed("", Duration.ofSeconds(1));

        // 那么 / Then — should still work with empty key + prefix
        verify(valueOperations).set("mq:idempotency:", "1", Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Should handle null event key for isProcessed")
    void shouldHandleNullEventKeyForIsProcessed() {
        // 当 / When
        when(redisTemplate.hasKey("mq:idempotency:null")).thenReturn(false);
        boolean processed = idempotencyService.isProcessed(null);

        // 那么 / Then
        assertThat(processed).isFalse();
    }
}
