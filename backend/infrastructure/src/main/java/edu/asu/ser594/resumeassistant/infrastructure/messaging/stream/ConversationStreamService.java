package edu.asu.ser594.resumeassistant.infrastructure.messaging.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 对话流式回复服务
 * Conversation stream reply service
 *
 * <p>通过内存中的 CompletableFuture 桥接 MQ 异步 AI 回复与 HTTP 流式请求。
 * Bridges MQ async AI replies with HTTP streaming requests via in-memory CompletableFuture.</p>
 *
 * <p><b>当前为伪流式传输（Pseudo-Streaming）</b>：
 * AI Service 通过 MQ 一次性返回完整回复，后端保持 HTTP 连接等待后将完整内容写入响应流。
 * 若后续 AI Service 支持真正流式生成，本服务的接口签名可复用，仅需将 awaitReply 的
 * 返回类型改为逐 chunk 推送的流即可。</p>
 */
@Service
@Slf4j
public class ConversationStreamService {

    private static final long DEFAULT_TIMEOUT_SECONDS = 60L;
    /**
     * 等待中的回复 future / Pending reply futures
     */
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingReplies = new ConcurrentHashMap<>();
    /**
     * 已到达但尚未被 stream 请求消费的早期回复 / Early replies arrived before stream request
     */
    private final ConcurrentHashMap<String, String> earlyReplies = new ConcurrentHashMap<>();

    /**
     * 等待指定对话的 AI 回复
     * Wait for AI reply of the specified conversation
     *
     * @param conversationId 对话 ID / Conversation ID
     * @return AI 回复内容 / AI reply content, or null if timed out
     */
    public String awaitReply(String conversationId) {
        return awaitReply(conversationId, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 等待指定对话的 AI 回复（带超时）
     * Wait for AI reply of the specified conversation with timeout
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param timeout        超时时间 / Timeout duration
     * @param unit           时间单位 / Time unit
     * @return AI 回复内容 / AI reply content, or null if timed out or failed
     */
    public String awaitReply(String conversationId, long timeout, TimeUnit unit) {
        // 1. 检查是否有早期已到达的回复 / Check for early arrived reply
        String earlyReply = earlyReplies.remove(conversationId);
        if (earlyReply != null) {
            log.info("Found early reply for conversation: {}, returning immediately", conversationId);
            return earlyReply;
        }

        // 2. 注册新的 CompletableFuture / Register new CompletableFuture
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> existing = pendingReplies.putIfAbsent(conversationId, future);

        if (existing != null) {
            // 已有其他请求在等待，复用同一个 future / Another request is already waiting
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

    /**
     * 完成指定对话的 AI 回复（由 MQ 监听器调用）
     * Complete AI reply for the specified conversation (called by MQ listener)
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param content        AI 回复内容 / AI reply content
     */
    public void completeReply(String conversationId, String content) {
        CompletableFuture<String> future = pendingReplies.remove(conversationId);
        if (future != null) {
            future.complete(content);
            log.info("Completed stream reply for conversation: {}", conversationId);
        } else {
            // Stream 请求尚未到达，暂存为早期回复 / Stream request not yet arrived, store as early reply
            earlyReplies.put(conversationId, content);
            log.info("No pending stream for conversation: {}, stored as early reply", conversationId);
        }
    }

    /**
     * 标记指定对话的回复失败（由 MQ 监听器调用）
     * Mark reply as failed for the specified conversation (called by MQ listener)
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param errorMessage   错误信息 / Error message
     */
    public void failReply(String conversationId, String errorMessage) {
        CompletableFuture<String> future = pendingReplies.remove(conversationId);
        if (future != null) {
            future.completeExceptionally(new RuntimeException(errorMessage));
            log.warn("Failed stream reply for conversation: {}, error: {}", conversationId, errorMessage);
        } else {
            // Stream 请求尚未到达，暂存错误信息以便返回 / Stream request not yet arrived
            earlyReplies.put(conversationId, "[Error] " + errorMessage);
            log.warn("No pending stream for conversation: {}, stored error as early reply: {}",
                    conversationId, errorMessage);
        }
    }
}
