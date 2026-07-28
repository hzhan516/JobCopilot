package io.jobcopilot.resumeassistant.domain.conversation.entity;

import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.exception.AiReplyInProgressException;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyState;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.AiReplyStatus;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.shared.entity.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root that manages the dialogue between user and AI around a specific resume-job pair.
 * 管理用户与 AI 围绕特定简历-职位组合进行对话的聚合根。
 */
public class Conversation extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID userId;
    private final UUID resumeVersionId;
    private final UUID jobId;
    private UUID aiOptimizedVersionId;
    private final LocalDateTime createdAt;
    private final List<Message> messages;
    private String title;
    private ConversationStatus status;
    private LocalDateTime updatedAt;
    private long version;
    private int contextTokens;
    private long totalTokensUsed;
    private String contextSummary;
    private int compactedThroughSequence;
    private AiReplyState aiReplyState;
    private UUID compactionRequestId;

    protected Conversation(UUID id, UUID userId, String title, ConversationStatus status,
                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           List<Message> messages, long version,
                           int contextTokens, long totalTokensUsed,
                           String contextSummary, int compactedThroughSequence,
                           AiReplyState aiReplyState, UUID compactionRequestId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.status = status;
        this.resumeVersionId = resumeVersionId;
        this.jobId = jobId;
        this.aiOptimizedVersionId = aiOptimizedVersionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.version = version;
        this.contextTokens = contextTokens;
        this.totalTokensUsed = totalTokensUsed;
        this.contextSummary = contextSummary;
        this.compactedThroughSequence = compactedThroughSequence;
        this.aiReplyState = aiReplyState != null ? aiReplyState : AiReplyState.idle();
        this.compactionRequestId = compactionRequestId;
    }

    public static Conversation create(UUID userId, String title, UUID resumeVersionId, UUID jobId) {
        String finalTitle = (title == null || title.trim().isEmpty()) ? "New Conversation" : title;
        LocalDateTime now = LocalDateTime.now();
        return new Conversation(
                UUID.randomUUID(),
                userId,
                finalTitle,
                ConversationStatus.ACTIVE,
                resumeVersionId,
                jobId,
                null,  // aiOptimizedVersionId initially null | AI 优化版本 ID 初始为空
                now,
                now,
                new ArrayList<>(),
                0L,
                0,     // contextTokens
                0L,    // totalTokensUsed
                null,  // contextSummary
                0,     // compactedThroughSequence
                AiReplyState.idle(),
                null
        );
    }

    /**
     * Reconstructs an aggregate from persistence data; no business invariants are enforced.
     * 从持久化数据重建聚合，不执行业务不变量校验。
     */
    public static Conversation reconstruct(UUID id, UUID userId, String title, ConversationStatus status,
                                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt,
                                           List<Message> messages, long version,
                                           int contextTokens, long totalTokensUsed,
                                           String contextSummary, int compactedThroughSequence,
                                           AiReplyState aiReplyState, UUID compactionRequestId) {
        return new Conversation(id, userId, title, status, resumeVersionId, jobId, aiOptimizedVersionId, createdAt, updatedAt, messages, version,
                contextTokens, totalTokensUsed, contextSummary, compactedThroughSequence, aiReplyState,
                compactionRequestId);
    }

    public static Conversation reconstruct(UUID id, UUID userId, String title, ConversationStatus status,
                                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt,
                                           List<Message> messages, long version,
                                           int contextTokens, long totalTokensUsed,
                                           String contextSummary, int compactedThroughSequence,
                                           AiReplyState aiReplyState) {
        return reconstruct(id, userId, title, status, resumeVersionId, jobId, aiOptimizedVersionId,
                createdAt, updatedAt, messages, version, contextTokens, totalTokensUsed,
                contextSummary, compactedThroughSequence, aiReplyState, null);
    }

    /** Backward-compatible reconstruction for tests and adapters that predate durable AI state. */
    public static Conversation reconstruct(UUID id, UUID userId, String title, ConversationStatus status,
                                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt,
                                           List<Message> messages, long version,
                                           int contextTokens, long totalTokensUsed,
                                           String contextSummary, int compactedThroughSequence) {
        return reconstruct(id, userId, title, status, resumeVersionId, jobId, aiOptimizedVersionId,
                createdAt, updatedAt, messages, version, contextTokens, totalTokensUsed,
                contextSummary, compactedThroughSequence, AiReplyState.idle());
    }

    public void addMessage(MessageRole role, String content) {
        addMessage(role, content, null);
    }

    public void addMessage(MessageRole role, String content, String fileUrl) {
        if (this.status == ConversationStatus.CLOSED) {
            throw new ConversationException("conversation.message.send.failed");
        }
        int sequence = this.messages.size() + 1;
        Message newMessage = Message.create(this.getId(), role, content, sequence, fileUrl);
        this.messages.add(newMessage);
        this.updatedAt = LocalDateTime.now();
    }

    /** Starts exactly one durable AI reply request for the latest persisted user message. */
    public void requestAiReply(UUID requestId, int userMessageSequence) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
        if (this.status == ConversationStatus.CLOSED) {
            throw new ConversationException("conversation.message.send.failed");
        }
        if (aiReplyState.isPending()) {
            throw new AiReplyInProgressException();
        }
        this.aiReplyState = AiReplyState.pending(requestId, userMessageSequence, LocalDateTime.now());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Completes the matching request and appends its assistant message exactly once.
     * Duplicate or stale results are ignored and return false.
     */
    public boolean completeAiReply(UUID requestId, String content, String fileUrl,
                                   int promptTokens, int completionTokens) {
        if (!aiReplyState.isPending() || !aiReplyState.matches(requestId)) {
            return false;
        }
        addMessage(MessageRole.ASSISTANT, content, fileUrl);
        int assistantSequence = messages.get(messages.size() - 1).getSequence();
        recordTokenUsage(promptTokens, completionTokens);
        LocalDateTime now = LocalDateTime.now();
        this.aiReplyState = new AiReplyState(requestId, AiReplyStatus.COMPLETED, null,
                aiReplyState.startedAt(), now, aiReplyState.userMessageSequence(), assistantSequence);
        this.updatedAt = now;
        return true;
    }

    /** Marks the matching request failed without manufacturing an assistant message. */
    public boolean failAiReply(UUID requestId, String errorCode) {
        if (!aiReplyState.isPending() || !aiReplyState.matches(requestId)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        this.aiReplyState = new AiReplyState(requestId, AiReplyStatus.FAILED, errorCode,
                aiReplyState.startedAt(), now, aiReplyState.userMessageSequence(), null);
        this.updatedAt = now;
        return true;
    }

    /** Marks the matching request timed out; a late result can no longer mutate the aggregate. */
    public boolean timeoutAiReply(UUID requestId) {
        if (!aiReplyState.isPending() || !aiReplyState.matches(requestId)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        this.aiReplyState = new AiReplyState(requestId, AiReplyStatus.TIMED_OUT, "END_TO_END_TIMEOUT",
                aiReplyState.startedAt(), now, aiReplyState.userMessageSequence(), null);
        this.updatedAt = now;
        return true;
    }

    /** Retries the last failed/timed-out user turn with a new request id. */
    public void retryAiReply(UUID newRequestId) {
        if (aiReplyState.status() != AiReplyStatus.FAILED
                && aiReplyState.status() != AiReplyStatus.TIMED_OUT) {
            throw new ConversationException("conversation.ai.reply.retry.not.allowed");
        }
        Integer userSequence = aiReplyState.userMessageSequence();
        if (userSequence == null) {
            throw new ConversationException("conversation.ai.reply.retry.not.allowed");
        }
        this.aiReplyState = AiReplyState.pending(newRequestId, userSequence, LocalDateTime.now());
        this.updatedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeTitle(String newTitle) {
        if (newTitle != null && !newTitle.trim().isEmpty()) {
            this.title = newTitle;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Derives the title from the first user message when the default title is still in place.
     * 当标题仍为默认值时，从首条用户消息中提取内容作为标题。
     */
    public void autoGenerateTitle(String content) {
        if (content != null && !content.trim().isEmpty()) {
            String trimmed = content.trim();
            String generated = trimmed.length() > 30 ? trimmed.substring(0, 30) + "..." : trimmed;
            if ("New Conversation".equals(this.title) || this.title == null || this.title.trim().isEmpty()) {
                this.title = generated;
                this.updatedAt = LocalDateTime.now();
            }
        }
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Exposes messages through an unmodifiable view to prevent external mutation of the aggregate state.
     * 通过不可修改视图暴露消息列表，防止外部代码篡改聚合内部状态。
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(this.messages);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public UUID getResumeVersionId() {
        return resumeVersionId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getAiOptimizedVersionId() {
        return aiOptimizedVersionId;
    }

    public void setAiOptimizedVersionId(UUID aiOptimizedVersionId) {
        this.aiOptimizedVersionId = aiOptimizedVersionId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public int getContextTokens() {
        return contextTokens;
    }

    public long getTotalTokensUsed() {
        return totalTokensUsed;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public int getCompactedThroughSequence() {
        return compactedThroughSequence;
    }

    public AiReplyState getAiReplyState() {
        return aiReplyState;
    }

    public UUID getCompactionRequestId() {
        return compactionRequestId;
    }

    /**
     * Records token usage from an AI call, updating both the snapshot context tokens
     * and the cumulative total.
     * 记录 AI 调用的 token 用量，同时更新快照上下文 token 和累计总量。
     */
    public void recordTokenUsage(int promptTokens, int completionTokens) {
        this.contextTokens = promptTokens;
        this.totalTokensUsed += (promptTokens + completionTokens);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Applies a compaction result: stores an LLM-generated summary of messages up to the
     * given sequence number, and recalculates context tokens from the remaining messages.
     * 应用压缩结果：存储 LLM 生成的截至指定序号的消息摘要，并重新计算剩余消息的上下文 token。
     */
    public void applyCompaction(String summary, int throughSequence, int newContextTokens) {
        this.contextSummary = summary;
        this.compactedThroughSequence = throughSequence;
        this.contextTokens = newContextTokens;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns whether a compaction is currently in progress (contextSummary is set but
     * the caller has not yet applied the final result).
     * 返回压缩是否正在进行中。
     */
    public boolean isCompacting() {
        return this.status == ConversationStatus.COMPACTING;
    }

    public void markCompacting(UUID requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
        this.status = ConversationStatus.COMPACTING;
        this.compactionRequestId = requestId;
        this.updatedAt = LocalDateTime.now();
    }

    /** Applies only the result belonging to the currently active compaction request. */
    public boolean completeCompaction(UUID requestId, String summary, int throughSequence,
                                      int newContextTokens) {
        if (!isCompacting() || !java.util.Objects.equals(compactionRequestId, requestId)) {
            return false;
        }
        applyCompaction(summary, throughSequence, newContextTokens);
        this.status = ConversationStatus.ACTIVE;
        // Retain the terminal correlation ID so clients can verify the exact compaction.
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    /** Releases a failed compaction only when it matches the active request. */
    public boolean failCompaction(UUID requestId) {
        if (!isCompacting() || !java.util.Objects.equals(compactionRequestId, requestId)) {
            return false;
        }
        this.status = ConversationStatus.ACTIVE;
        // Retain the terminal correlation ID until the next compaction replaces it.
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    public void markActive() {
        this.status = ConversationStatus.ACTIVE;
        this.compactionRequestId = null;
        this.updatedAt = LocalDateTime.now();
    }
}
