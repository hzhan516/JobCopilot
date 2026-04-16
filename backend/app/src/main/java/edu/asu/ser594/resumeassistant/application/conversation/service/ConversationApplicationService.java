package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    private final ResumeFacade resumeFacade;

    /**
     * 创建对话
     * Create conversation
     */
    @Transactional
    public Conversation createConversation(CreateConversationCommand command) {
        log.info("Creating new conversation for user: {}", command.userId());
        
        if (command.resumeVersionId() != null) {
            try {
                resumeFacade.getVersion(command.resumeVersionId(), command.userId());
            } catch (Exception e) {
                throw new ConversationException(ConversationException.ErrorType.INVALID_RESUME_VERSION, "Invalid resume version or access denied");
            }
        }

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
     * 关闭对话
     * Close conversation
     */
    @Transactional
    public void closeConversation(UUID conversationId, UUID userId) {
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
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {}", conversationId);
        // Ensure ownership
        getConversationWithOwnershipCheck(conversationId, userId);
        conversationRepository.deleteById(conversationId);
    }

    /**
     * 获取对话并进行所有权校验
     * Get conversation and verify ownership
     */
    private Conversation getConversationWithOwnershipCheck(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationException(ConversationException.ErrorType.NOT_FOUND, "Conversation not found"));
        
        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException(ConversationException.ErrorType.ACCESS_DENIED, "Access denied");
        }
        
        return conversation;
    }
}
