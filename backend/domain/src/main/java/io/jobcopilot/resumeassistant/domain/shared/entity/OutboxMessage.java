package io.jobcopilot.resumeassistant.domain.shared.entity;

import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;
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
    private int attemptCount;
    private LocalDateTime nextAttemptAt;
    private String lastErrorCode;
    private LocalDateTime lockedAt;
    private String workerId;

    @Builder
    public OutboxMessage(String id,
                         String exchange,
                         String routingKey,
                         String payload,
                         OutboxStatus status,
                         LocalDateTime createdAt,
                         LocalDateTime sentAt,
                         int attemptCount,
                         LocalDateTime nextAttemptAt,
                         String lastErrorCode,
                         LocalDateTime lockedAt,
                         String workerId) {
        this.id = id;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastErrorCode = lastErrorCode;
        this.lockedAt = lockedAt;
        this.workerId = workerId;
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
                .attemptCount(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();
    }

    /**
     * 标记为已发送
     * Mark as sent
     */
    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lockedAt = null;
        this.workerId = null;
        this.lastErrorCode = null;
    }

    /**
     * 标记为发送失败
     * Mark as failed
     */
    public void markProcessing(String workerId, LocalDateTime now) {
        if (status != OutboxStatus.PENDING && status != OutboxStatus.FAILED) {
            throw new IllegalStateException("Only due outbox messages can be claimed");
        }
        this.status = OutboxStatus.PROCESSING;
        this.workerId = workerId;
        this.lockedAt = now;
        this.attemptCount += 1;
    }

    public void markFailed(String errorCode, LocalDateTime nextAttemptAt, int maxAttempts) {
        this.lastErrorCode = errorCode;
        this.lockedAt = null;
        this.workerId = null;
        if (attemptCount >= maxAttempts) {
            this.status = OutboxStatus.DEAD;
            this.nextAttemptAt = null;
        } else {
            this.status = OutboxStatus.FAILED;
            this.nextAttemptAt = nextAttemptAt;
        }
    }

    public void recoverStaleClaim(LocalDateTime now) {
        if (status == OutboxStatus.PROCESSING) {
            this.status = OutboxStatus.FAILED;
            this.lastErrorCode = "STALE_CLAIM";
            this.nextAttemptAt = now;
            this.lockedAt = null;
            this.workerId = null;
        }
    }
}
