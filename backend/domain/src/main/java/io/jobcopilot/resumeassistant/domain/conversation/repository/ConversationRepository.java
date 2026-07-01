package io.jobcopilot.resumeassistant.domain.conversation.repository;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 对话仓储接口
 * Conversation repository interface
 */
public interface ConversationRepository {

    /**
     * 保存对话
     * Save conversation
     */
    Conversation save(Conversation conversation);

    /**
     * 根据 ID 查找对话
     * Find conversation by ID
     */
    Optional<Conversation> findById(UUID id);

    /**
     * 查找某用户的所有对话
     * Find all conversations by user ID
     */
    List<Conversation> findAllByUserId(UUID userId);

    /**
     * 统计某用户的对话数量
     * Count conversations by user ID
     */
    long countByUserId(UUID userId);

    /**
     * 统计所有对话数量
     * Count all conversations
     */
    long count();

    /**
     * 根据 ID 删除对话
     * Delete conversation by ID
     */
    void deleteById(UUID id);
}
