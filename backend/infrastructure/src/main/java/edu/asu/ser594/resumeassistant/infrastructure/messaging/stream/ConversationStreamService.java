package edu.asu.ser594.resumeassistant.infrastructure.messaging.stream;

import edu.asu.ser594.resumeassistant.api.conversation.port.ConversationStreamPort;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bridges MQ asynchronous AI replies with HTTP streaming requests via in-memory CompletableFuture
 * and Redis Pub/Sub for cross-instance coordination.
 *
 * <p><b>Design rationale</b>: In a multi-instance backend deployment, the HTTP long-polling request
 * may land on instance A while the MQ reply is consumed by instance B. Redis provides the
 * cross-process coordination layer without replacing the local fast path.</p>
 *
 * <p>Pseudo-Streaming: the AI Service currently returns the full reply through MQ in one shot;
 * the backend holds the HTTP connection, awaits the future, and then writes the entire content.
 * If the AI Service later supports true streaming, only the internal implementation of awaitReply
 * needs to change—the port contract remains the same.</p>
 * 通过内存 CompletableFuture 与 Redis Pub/Sub 桥接 MQ 异步 AI 回复与 HTTP 流式请求。
 * 当前为伪流式：AI Service 通过 MQ 一次性返回完整回复，后端保持连接等待后写入；
 * 若后续支持真正流式生成，仅需修改 awaitReply 内部实现，接口签名可复用
 */
@Service
@Slf4j
public class ConversationStreamService implements ConversationStreamPort, MessageListener {

    private static final long DEFAULT_TIMEOUT_SECONDS = 60L;
    private static final String PENDING_PREFIX = "ra:conv:pending:";
    private static final String EARLY_PREFIX = "ra:conv:early:";
    private static final String CHANNEL = "ra:conv:reply";

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer redisContainer;

    // 本地快速路径 / Local fast path
    private final ConcurrentHashMap<String, CompletableFuture<String>> localPending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> localEarly = new ConcurrentHashMap<>();

    public ConversationStreamService(StringRedisTemplate redisTemplate,
                                     @Autowired(required = false) RedisMessageListenerContainer redisContainer) {
        this.redisTemplate = redisTemplate;
        this.redisContainer = redisContainer;
    }

    @PostConstruct
    public void init() {
        if (redisContainer != null) {
            redisContainer.addMessageListener(this, new PatternTopic(CHANNEL));
            log.info("Subscribed to Redis Pub/Sub channel: {}", CHANNEL);
        } else {
            log.warn("RedisMessageListenerContainer is not available; cross-instance Pub/Sub bridging disabled.");
        }
    }

    public String awaitReply(String conversationId) {
        return awaitReply(conversationId, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public String awaitReply(String conversationId, long timeout, TimeUnit unit) {
        // 1. 检查本地 early reply / Check local early reply
        String early = localEarly.remove(conversationId);
        if (early != null) {
            log.info("Found local early reply for conversation: {}, returning immediately", conversationId);
            return early;
        }

        // 2. 检查 Redis early reply（其他实例提前到达）/ Check Redis early reply (arrived on another instance)
        String redisEarly = redisTemplate.opsForValue().get(EARLY_PREFIX + conversationId);
        if (redisEarly != null) {
            redisTemplate.delete(EARLY_PREFIX + conversationId);
            log.info("Found Redis early reply for conversation: {}, returning immediately", conversationId);
            return redisEarly;
        }

        // 3. 注册本地 pending future，并设置 Redis 心跳标记 / Register local pending future and Redis heartbeat
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> existing = localPending.putIfAbsent(conversationId, future);
        if (existing != null) {
            future = existing;
            log.info("Reusing existing pending future for conversation: {}", conversationId);
        } else {
            log.info("Registered pending stream for conversation: {}, timeout={} {}",
                    conversationId, timeout, unit);
            redisTemplate.opsForValue().set(
                    PENDING_PREFIX + conversationId,
                    "1",
                    Duration.ofSeconds(timeout + 5)
            );
        }

        try {
            return future.get(timeout, unit);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Stream timeout for conversation: {} after {} {}", conversationId, timeout, unit);
            localPending.remove(conversationId, future);
            redisTemplate.delete(PENDING_PREFIX + conversationId);
            return null;
        } catch (InterruptedException e) {
            log.warn("Stream interrupted for conversation: {}", conversationId);
            Thread.currentThread().interrupt();
            localPending.remove(conversationId, future);
            redisTemplate.delete(PENDING_PREFIX + conversationId);
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            log.warn("Stream failed for conversation: {}", conversationId, e.getCause());
            localPending.remove(conversationId, future);
            redisTemplate.delete(PENDING_PREFIX + conversationId);
            return null;
        }
    }

    public void completeReply(String conversationId, String content) {
        // 1. 尝试完成本地 future / Try to complete local future
        CompletableFuture<String> future = localPending.remove(conversationId);
        if (future != null) {
            future.complete(content);
            log.info("Completed local stream reply for conversation: {}", conversationId);
            return;
        }

        // 2. 检查 Redis 是否有 pending 标记（说明请求在其他实例上等待）/ Check if pending on another instance
        Boolean hasPending = redisTemplate.hasKey(PENDING_PREFIX + conversationId);
        if (Boolean.TRUE.equals(hasPending)) {
            redisTemplate.convertAndSend(CHANNEL, conversationId + "|CONTENT|" + content);
            log.info("Broadcasted reply for conversation: {} to other instances", conversationId);
            return;
        }

        // 3. 请求尚未到达，存入 Redis early reply / Store as early reply
        redisTemplate.opsForValue().set(
                EARLY_PREFIX + conversationId,
                content,
                Duration.ofMinutes(2)
        );
        log.info("No pending stream for conversation: {}, stored as Redis early reply", conversationId);
    }

    public void failReply(String conversationId, String errorMessage) {
        CompletableFuture<String> future = localPending.remove(conversationId);
        if (future != null) {
            future.completeExceptionally(new RuntimeException(errorMessage));
            log.warn("Failed local stream reply for conversation: {}, error: {}", conversationId, errorMessage);
            return;
        }

        Boolean hasPending = redisTemplate.hasKey(PENDING_PREFIX + conversationId);
        if (Boolean.TRUE.equals(hasPending)) {
            redisTemplate.convertAndSend(CHANNEL, conversationId + "|ERROR|" + errorMessage);
            log.warn("Broadcasted error for conversation: {} to other instances", conversationId);
            return;
        }

        redisTemplate.opsForValue().set(
                EARLY_PREFIX + conversationId,
                "[Error] " + errorMessage,
                Duration.ofMinutes(2)
        );
        log.warn("No pending stream for conversation: {}, stored error as Redis early reply: {}",
                conversationId, errorMessage);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        String[] parts = body.split("\\|", 3);
        if (parts.length != 3) {
            log.warn("Invalid Redis Pub/Sub message format: {}", body);
            return;
        }

        String conversationId = parts[0];
        String type = parts[1];
        String payload = parts[2];

        CompletableFuture<String> future = localPending.remove(conversationId);
        if (future == null) {
            log.debug("Received remote reply for conversation: {} but no local pending future", conversationId);
            return;
        }

        if ("CONTENT".equals(type)) {
            future.complete(payload);
            log.info("Completed remote stream reply for conversation: {}", conversationId);
        } else {
            future.completeExceptionally(new RuntimeException(payload));
            log.warn("Failed remote stream reply for conversation: {}, error: {}", conversationId, payload);
        }
        redisTemplate.delete(PENDING_PREFIX + conversationId);
    }
}
