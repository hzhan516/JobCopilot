package edu.asu.ser594.resumeassistant.domain.conversation.entity;

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
public class Conversation extends AggregateRoot<String> {

    private final String id;
    private final String userId;
    private String title;
    private ConversationStatus status;
    private final String resumeVersionId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<Message> messages;

    /**
     * 保护级别的原生构造函数
     * Protected native constructor
     */
    protected Conversation(String id, String userId, String title, ConversationStatus status, 
                           String resumeVersionId, LocalDateTime createdAt, LocalDateTime updatedAt, 
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
    public static Conversation create(String userId, String title, String resumeVersionId) {
        String finalTitle = (title == null || title.trim().isEmpty()) ? "New Conversation" : title;
        LocalDateTime now = LocalDateTime.now();
        return new Conversation(
            UUID.randomUUID().toString(),
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
    public static Conversation reconstruct(String id, String userId, String title, ConversationStatus status, 
                                           String resumeVersionId, LocalDateTime createdAt, LocalDateTime updatedAt, 
                                           List<Message> messages) {
        return new Conversation(id, userId, title, status, resumeVersionId, createdAt, updatedAt, messages);
    }

    /**
     * 领域行为：添加新消息
     * Domain behavior: add new message
     */
    public void addMessage(MessageRole role, String content) {
        if (this.status == ConversationStatus.CLOSED) {
            throw new IllegalStateException("Cannot add message to a closed conversation");
        }
        int sequence = this.messages.size() + 1;
        Message newMessage = Message.create(this.getId(), role, content, sequence);
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
     * 领域行为：检查是否属于指定用户
     * Domain behavior: check if owned by specific user
     */
    public boolean isOwnedBy(String userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 获取所有消息（返回不可变列表保护内部状态）
     * Get all messages (returns unmodifiable list to protect internal state)
     */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(this.messages);
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public String getResumeVersionId() {
        return resumeVersionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
