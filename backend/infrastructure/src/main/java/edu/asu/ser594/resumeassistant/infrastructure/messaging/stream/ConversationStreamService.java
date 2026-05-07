package edu.asu.ser594.resumeassistant.infrastructure.messaging.stream;

import edu.asu.ser594.resumeassistant.api.conversation.port.ConversationStreamPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Bridges MQ asynchronous AI replies with HTTP streaming requests via in-memory CompletableFuture.
 *
 * <p><b>Pseudo-Streaming</b>: the AI Service currently returns the full reply through MQ in one shot;
 * the backend holds the HTTP connection, awaits the future, and then writes the entire content.
 * If the AI Service later supports true streaming, only the internal implementation of awaitReply
 * needs to change—the port contract remains the same.</p>
 * 通过内存 CompletableFuture 桥接 MQ 异步 AI 回复与 HTTP 流式请求。
 * 当前为伪流式：AI Service 通过 MQ 一次性返回完整回复，后端保持连接等待后写入；
 * 若后续支持真正流式生成，仅需修改 awaitReply 内部实现，接口签名可复用
 */
@Service
@Slf4j
public class ConversationStreamService implements ConversationStreamPort {

    private static final long DEFAULT_TIMEOUT_SECONDS = 60L;

    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingReplies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> earlyReplies = new ConcurrentHashMap<>();

    public String awaitReply(String conversationId) {
        return awaitReply(conversationId, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public String awaitReply(String conversationId, long timeout, TimeUnit unit) {
        String earlyReply = earlyReplies.remove(conversationId);
        if (earlyReply != null) {
            log.info("Found early reply for conversation: {}, returning immediately", conversationId);
            return earlyReply;
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> existing = pendingReplies.putIfAbsent(conversationId, future);

        if (existing != null) {
            future = existing;
            log.info("Reusing existing pending future for conversation: {}", conversationId);
        } else {
            log.info("Registered pending stream for conversation: {}, timeout={} {}",
                    conversationId, timeout, unit);
        }

        try {
            return future.get(timeout, unit);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Stream timeout for conversation: {} after {} {}", conversationId, timeout, unit);
            pendingReplies.remove(conversationId, future);
            return null;
        } catch (InterruptedException e) {
            log.warn("Stream interrupted for conversation: {}", conversationId);
            Thread.currentThread().interrupt();
            pendingReplies.remove(conversationId, future);
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            log.warn("Stream failed for conversation: {}", conversationId, e.getCause());
            pendingReplies.remove(conversationId, future);
            return null;
        }
    }

    public void completeReply(String conversationId, String content) {
        CompletableFuture<String> future = pendingReplies.remove(conversationId);
        if (future != null) {
            future.complete(content);
            log.info("Completed stream reply for conversation: {}", conversationId);
        } else {
            // Stream request has not arrived yet; store so the pending await can pick it up immediately
            // Stream 请求尚未到达，暂存为早期回复以便后续请求立即获取
            earlyReplies.put(conversationId, content);
            log.info("No pending stream for conversation: {}, stored as early reply", conversationId);
        }
    }

    public void failReply(String conversationId, String errorMessage) {
        CompletableFuture<String> future = pendingReplies.remove(conversationId);
        if (future != null) {
            future.completeExceptionally(new RuntimeException(errorMessage));
            log.warn("Failed stream reply for conversation: {}, error: {}", conversationId, errorMessage);
        } else {
            earlyReplies.put(conversationId, "[Error] " + errorMessage);
            log.warn("No pending stream for conversation: {}, stored error as early reply: {}",
                    conversationId, errorMessage);
        }
    }
}
