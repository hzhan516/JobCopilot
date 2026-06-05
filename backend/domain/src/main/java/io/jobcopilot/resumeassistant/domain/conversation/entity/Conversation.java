package io.jobcopilot.resumeassistant.domain.conversation.entity;

import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
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

    protected Conversation(UUID id, UUID userId, String title, ConversationStatus status,
                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           List<Message> messages, long version) {
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
                0L
        );
    }

    /**
     * Reconstructs an aggregate from persistence data; no business invariants are enforced.
     * 从持久化数据重建聚合，不执行业务不变量校验。
     */
    public static Conversation reconstruct(UUID id, UUID userId, String title, ConversationStatus status,
                                           UUID resumeVersionId, UUID jobId, UUID aiOptimizedVersionId,
                                           LocalDateTime createdAt, LocalDateTime updatedAt,
                                           List<Message> messages, long version) {
        return new Conversation(id, userId, title, status, resumeVersionId, jobId, aiOptimizedVersionId, createdAt, updatedAt, messages, version);
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
}
