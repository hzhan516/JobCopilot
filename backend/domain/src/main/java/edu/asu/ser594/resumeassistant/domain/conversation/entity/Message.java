package edu.asu.ser594.resumeassistant.domain.conversation.entity;

import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息实体 (实体类)
 * Message entity
 */
public class Message implements Entity<String> {

    private final String id;
    private final String conversationId;
    private final MessageRole role;
    private final String content;
    private final int sequence;
    private final LocalDateTime createdAt;

    /**
     * 保护级别的原生构造函数
     * Protected native constructor
     */
    protected Message(String id, String conversationId, MessageRole role, String content, int sequence, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.sequence = sequence;
        this.createdAt = createdAt;
    }

    /**
     * 静态工厂方法，用于创建新消息
     * Static factory method for creating a new message
     */
    public static Message create(String conversationId, MessageRole role, String content, int sequence) {
        return new Message(
            UUID.randomUUID().toString(),
            conversationId,
            role,
            content,
            sequence,
            LocalDateTime.now()
        );
    }

    /**
     * 从仓储恢复消息实体
     * Reconstruct message entity from repository
     */
    public static Message reconstruct(String id, String conversationId, MessageRole role, String content, int sequence, LocalDateTime createdAt) {
        return new Message(id, conversationId, role, content, sequence, createdAt);
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getConversationId() {
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
}
