package edu.asu.ser594.resumeassistant.domain.conversation.entity;

import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 对话聚合根
 * Conversation aggregate root
 */
public class Conversation extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID userId;
    private String title;
    private ConversationStatus status;
    private final UUID resumeVersionId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<Message> messages;

    /**
     * 保护级别的原生构造函数
     * Protected native constructor
     */
    protected Conversation(UUID id, UUID userId, String title, ConversationStatus status, 
                           UUID resumeVersionId, LocalDateTime createdAt, LocalDateTime updatedAt, 
                           List<Message> messages) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.status = status;
        this.resumeVersionId = resumeVersionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    /**
     * 静态工厂方法：创建新对话
     * Static factory method: create new conversation
     */
    public static Conversation create(UUID userId, String title, UUID resumeVersionId) {
        String finalTitle = (title == null || title.trim().isEmpty()) ? "New Conversation" : title;
        LocalDateTime now = LocalDateTime.now();
        return new Conversation(
            UUID.randomUUID(),
            userId,
            finalTitle,
            ConversationStatus.ACTIVE,
            resumeVersionId,
            now,
            now,
            new ArrayList<>()
        );
    }

    /**
     * 从仓储恢复聚合根
     * Reconstruct aggregate root from repository
     */
    public static Conversation reconstruct(UUID id, UUID userId, String title, ConversationStatus status, 
                                           UUID resumeVersionId, LocalDateTime createdAt, LocalDateTime updatedAt, 
                                           List<Message> messages) {
        return new Conversation(id, userId, title, status, resumeVersionId, createdAt, updatedAt, messages);
    }

    /**
     * 领域行为：添加新消息
     * Domain behavior: add new message
     */
    public void addMessage(MessageRole role, String content) {
        addMessage(role, content, null);
    }

    /**
     * 领域行为：添加新消息（支持附带文件链接）
     * Domain behavior: add new message with optional file URL
     */
    public void addMessage(MessageRole role, String content, String fileUrl) {
        if (this.status == ConversationStatus.CLOSED) {
            throw new ConversationException(ConversationException.ErrorType.CLOSED_CONVERSATION, "Cannot add message to a closed conversation");
        }
        int sequence = this.messages.size() + 1;
        Message newMessage = Message.create(this.getId(), role, content, sequence, fileUrl);
        this.messages.add(newMessage);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 领域行为：关闭对话
     * Domain behavior: close conversation
     */
    public void close() {
        this.status = ConversationStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 领域行为：修改对话标题
     * Domain behavior: change conversation title
     */
    public void changeTitle(String newTitle) {
        if (newTitle != null && !newTitle.trim().isEmpty()) {
            this.title = newTitle;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 领域行为：根据首条消息自动生成标题
     * Domain behavior: auto-generate title from first message
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

    /**
     * 领域行为：检查是否属于指定用户
     * Domain behavior: check if owned by specific user
     */
    public boolean isOwnedBy(UUID userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * 获取所有消息（返回不可变列表保护内部状态）
     * Get all messages (returns unmodifiable list to protect internal state)
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
