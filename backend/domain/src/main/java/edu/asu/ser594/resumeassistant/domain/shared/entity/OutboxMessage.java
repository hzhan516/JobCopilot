package edu.asu.ser594.resumeassistant.domain.shared.entity;

import edu.asu.ser594.resumeassistant.types.enums.OutboxStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox 消息实体
 * Outbox message entity
 * <p>
 * 用于事务性消息投递，保证数据库操作与消息记录的原子性。
 * Used for transactional message delivery to ensure atomicity between DB operations and message recording.
 */
@Getter
public class OutboxMessage {

    private final String id;
    private final String exchange;
    private final String routingKey;
    private final String payload;
    private final LocalDateTime createdAt;
    private OutboxStatus status;
    private LocalDateTime sentAt;

    @Builder
    public OutboxMessage(String id,
                         String exchange,
                         String routingKey,
                         String payload,
                         OutboxStatus status,
                         LocalDateTime createdAt,
                         LocalDateTime sentAt) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    /**
     * 创建待发送的 Outbox 消息
     * Create a pending outbox message
     */
    public static OutboxMessage createPending(String exchange, String routingKey, String payload) {
        return OutboxMessage.builder()
                .id(UUID.randomUUID().toString())
                .exchange(exchange)
                .routingKey(routingKey)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 标记为已发送
     * Mark as sent
     */
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * 标记为发送失败
     * Mark as failed
     */
    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
