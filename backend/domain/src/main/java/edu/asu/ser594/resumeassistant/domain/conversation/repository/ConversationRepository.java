package edu.asu.ser594.resumeassistant.domain.conversation.repository;

import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;

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
     * 根据 ID 删除对话
     * Delete conversation by ID
     */
    void deleteById(UUID id);
}
