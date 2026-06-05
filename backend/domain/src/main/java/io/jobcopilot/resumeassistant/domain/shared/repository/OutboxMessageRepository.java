package io.jobcopilot.resumeassistant.domain.shared.repository;

import io.jobcopilot.resumeassistant.domain.shared.entity.OutboxMessage;
import io.jobcopilot.resumeassistant.types.enums.OutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 消息仓储接口
 * Outbox message repository interface
 */
public interface OutboxMessageRepository {

    /**
     * 保存 Outbox 消息
     * Save outbox message
     */
    OutboxMessage save(OutboxMessage message);

    /**
     * 根据状态查询消息
     * Find messages by status
     */
    List<OutboxMessage> findByStatus(OutboxStatus status);

    /**
     * 删除指定状态且发送时间早于 cutoff 的消息
     * Delete messages with given status and sent before cutoff
     */
    void deleteByStatusAndSentAtBefore(OutboxStatus status, LocalDateTime cutoff);
}
