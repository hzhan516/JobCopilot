package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.application.conversation.query.GetConversationQuery;
import io.jobcopilot.resumeassistant.application.conversation.query.ListConversationsQuery;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.query.ConversationSummary;
import io.jobcopilot.resumeassistant.domain.conversation.query.ConversationDetail;
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

    public ConversationDetail getConversationDetail(GetConversationQuery query) {
        int page = query.page() == null ? 0 : Math.max(0, query.page());
        int size = query.size() == null ? 50 : Math.min(100, Math.max(1, query.size()));
        ConversationDetail detail = conversationRepository
                .findDetailById(query.conversationId(), page, size)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));
        if (!detail.userId().equals(query.userId())) {
            throw new ConversationException("access.denied");
        }
        return detail;
    }

    /**
     * 获取用户所有对话
     * List user conversations
     */
    public List<ConversationSummary> listConversations(ListConversationsQuery query) {
        log.info("Listing conversations for user: {}", query.userId());
        return conversationRepository.findSummariesByUserId(query.userId());
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
