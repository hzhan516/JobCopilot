package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.application.conversation.query.GetConversationQuery;
import edu.asu.ser594.resumeassistant.application.conversation.query.ListConversationsQuery;
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
 * 对话查询服务
 * Conversation query service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ConversationQueryService {

    private final ConversationRepository conversationRepository;

    /**
     * 获取对话
     * Get conversation
     */
    public Conversation getConversation(GetConversationQuery query) {
        return getConversationWithOwnershipCheck(query.conversationId(), query.userId());
    }

    /**
     * 获取用户所有对话
     * List user conversations
     */
    public List<Conversation> listConversations(ListConversationsQuery query) {
        log.info("Listing conversations for user: {}", query.userId());
        return conversationRepository.findAllByUserId(query.userId());
    }

    /**
     * 获取对话并进行所有权校验
     * Get conversation and verify ownership
     */
    private Conversation getConversationWithOwnershipCheck(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException("access.denied");
        }

        return conversation;
    }
}
