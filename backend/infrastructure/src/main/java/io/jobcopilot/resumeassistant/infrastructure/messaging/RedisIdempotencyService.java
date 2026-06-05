package io.jobcopilot.resumeassistant.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed idempotency guard for MQ consumers.
 * Prevents duplicate processing of the same event across consumer restarts or redeliveries.
 * 基于 Redis 的幂等性保障：防止 MQ 消息在消费者重启或重投递时被重复处理。
 */
@Component
@RequiredArgsConstructor
public class RedisIdempotencyService {

    private static final String KEY_PREFIX = "mq:idempotency:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Checks whether the given event key has already been processed.
     * 检查给定的事件键是否已被处理过。
     *
     * @param eventKey unique identifier for the event / 事件的唯一标识
     * @return true if the event was already processed / 若事件已被处理则返回 true
     */
    public boolean isProcessed(String eventKey) {
        String key = KEY_PREFIX + eventKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Marks the given event key as processed with the specified TTL.
     * 将给定的事件键标记为已处理，并设置过期时间。
     *
     * @param eventKey unique identifier for the event / 事件的唯一标识
     * @param ttl      time-to-live / 过期时间
     */
    public void markProcessed(String eventKey, Duration ttl) {
        String key = KEY_PREFIX + eventKey;
        redisTemplate.opsForValue().set(key, "1", ttl);
    }

    /**
     * Convenience overload using a default 24-hour TTL.
     * 使用默认 24 小时过期时间的便捷重载。
     *
     * @param eventKey unique identifier for the event / 事件的唯一标识
     */
    public void markProcessed(String eventKey) {
        markProcessed(eventKey, Duration.ofHours(24));
    }
}
