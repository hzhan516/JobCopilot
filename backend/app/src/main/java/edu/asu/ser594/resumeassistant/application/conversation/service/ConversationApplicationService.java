package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对话应用服务
 * Conversation application service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversationApplicationService {

    private final ConversationRepository conversationRepository;

    /**
     * 创建对话
     * Create conversation
     */
    @Transactional
    public Conversation createConversation(CreateConversationCommand command) {
        log.info("Creating new conversation for user: {}", command.userId());
        Conversation conversation = Conversation.create(
            command.userId(),
            command.title(),
            command.resumeVersionId()
        );
        return conversationRepository.save(conversation);
    }

    /**
     * 发送消息
     * Send message
     */
    @Transactional
    public Conversation sendMessage(SendMessageCommand command) {
        log.info("Sending message to conversation: {}", command.conversationId());
        Conversation conversation = getConversationWithOwnershipCheck(command.conversationId(), command.userId());
        
        conversation.addMessage(command.role(), command.content());
        return conversationRepository.save(conversation);
    }

    /**
     * 获取对话
     * Get conversation
     */
    public Conversation getConversation(String conversationId, String userId) {
        return getConversationWithOwnershipCheck(conversationId, userId);
    }

    /**
     * 获取用户所有对话
     * List user conversations
     */
    public List<Conversation> listConversations(String userId) {
        log.info("Listing conversations for user: {}", userId);
        return conversationRepository.findAllByUserId(userId);
    }

    /**
     * 关闭对话
     * Close conversation
     */
    @Transactional
    public void closeConversation(String conversationId, String userId) {
        log.info("Closing conversation: {}", conversationId);
        Conversation conversation = getConversationWithOwnershipCheck(conversationId, userId);
        conversation.close();
        conversationRepository.save(conversation);
    }

    /**
     * 删除对话
     * Delete conversation
     */
    @Transactional
    public void deleteConversation(String conversationId, String userId) {
        log.info("Deleting conversation: {}", conversationId);
        // Ensure ownership
        getConversationWithOwnershipCheck(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }

    /**
     * 获取对话并进行所有权校验
     * Get conversation and verify ownership
     */
    private Conversation getConversationWithOwnershipCheck(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found or access denied"));
        
        if (!conversation.isOwnedBy(userId)) {
            throw new IllegalArgumentException("Conversation not found or access denied");
        }
        
        return conversation;
    }
}
