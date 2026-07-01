package io.jobcopilot.resumeassistant.infrastructure.cache.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.config.repository.DynamicConfigRepository;
import io.jobcopilot.resumeassistant.infrastructure.cache.config.DynamicConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 动态配置 Redis Pub/Sub 监听器 / Redis Pub/Sub listener for dynamic config changes.
 * 
 * 订阅 {@code config:changed} 频道，收到通知后解析 key，从数据库加载最新值并写入本地缓存。
 * Subscribes to {@code config:changed} channel, parses the key, reloads the
 * latest value from the repository and writes it to the local cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicConfigListener implements MessageListener {

    private final DynamicConfigRepository dynamicConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received config change notification: {}", body);
        try {
            ConfigChangedEvent event = objectMapper.readValue(body, ConfigChangedEvent.class);
            if (event.key() == null || event.key().isBlank()) {
                log.warn("Config change event has no key, ignoring");
                return;
            }
            dynamicConfigRepository.findByKey(event.key())
                    .ifPresentOrElse(
                            cfg -> {
                                String value = cfg.isSensitive() ? "***" : cfg.getConfigValue();
                                DynamicConfigCache.put(cfg.getConfigKey(), value);
                                log.info("Dynamic config cache updated: {} = {}", cfg.getConfigKey(), value);
                            },
                            () -> log.warn("Config key not found in repository: {}", event.key())
                    );
        } catch (Exception e) {
            log.error("Failed to process config change message: {}", body, e);
        }
    }

    /**
     * 配置变更事件 / Config change event payload.
     */
    public record ConfigChangedEvent(String key) {
    }
}
