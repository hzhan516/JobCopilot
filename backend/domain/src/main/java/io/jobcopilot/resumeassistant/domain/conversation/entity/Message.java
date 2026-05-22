package io.jobcopilot.resumeassistant.domain.conversation.entity;

import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.domain.shared.entity.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息实体 (实体类)
 * Message entity
 */
public class Message implements Entity<UUID> {

    private final UUID id;
    private final UUID conversationId;
    private final MessageRole role;
    private final String content;
    private final int sequence;
    private final LocalDateTime createdAt;
    private final String fileUrl;

    /**
     * 保护级别的原生构造函数
     * Protected native constructor
     */
    protected Message(UUID id, UUID conversationId, MessageRole role, String content, int sequence, LocalDateTime createdAt, String fileUrl) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.fileUrl = fileUrl;
    }

    /**
     * 静态工厂方法，用于创建新消息
     * Static factory method for creating a new message
     */
    public static Message create(UUID conversationId, MessageRole role, String content, int sequence) {
        return create(conversationId, role, content, sequence, null);
    }

    /**
     * 静态工厂方法，用于创建附带文件链接的新消息
     * Static factory method for creating a new message with an optional file URL
     */
    public static Message create(UUID conversationId, MessageRole role, String content, int sequence, String fileUrl) {
        return new Message(
                UUID.randomUUID(),
                conversationId,
                role,
                content,
                sequence,
                LocalDateTime.now(),
                fileUrl
        );
    }

    /**
     * 从仓储恢复消息实体
     * Reconstruct message entity from repository
     */
    public static Message reconstruct(UUID id, UUID conversationId, MessageRole role, String content, int sequence, LocalDateTime createdAt, String fileUrl) {
        return new Message(id, conversationId, role, content, sequence, createdAt, fileUrl);
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getSequence() {
        return sequence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}
