package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ConversationCompactCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Orchestrates user-triggered conversation compaction via async MQ.
 * 编排用户触发的异步对话上下文压缩流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationCompactionService {

    private final ConversationRepository conversationRepository;
    private final AiMessagePublisherPort aiMessagePublisherPort;

    /**
     * Validates ownership, guards against concurrent compaction, and dispatches
     * the compact command to the AI service via MQ.
     * 验证所有权，防止并发压缩，并将压缩命令通过 MQ 发送至 AI 服务。
     */
    @Transactional(timeout = 30)
    public void requestCompaction(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        if (!conversation.isOwnedBy(userId)) {
            throw new ConversationException("access.denied");
        }

        if (conversation.getStatus() == ConversationStatus.COMPACTING) {
            log.warn("Compaction already in progress for conversation: {}", conversationId);
            throw new ConversationException("conversation.compaction.in.progress");
        }

        UUID requestId = UUID.randomUUID();
        conversation.markCompacting(requestId);
        conversationRepository.save(conversation);

        List<Map<String, Object>> history = buildCompactHistory(conversation);

        ConversationCompactCommand command = new ConversationCompactCommand(
                conversationId.toString(),
                userId.toString(),
                history,
                conversation.getCompactedThroughSequence(),
                requestId.toString()
        );
        aiMessagePublisherPort.sendConversationCompact(command);

        log.info("Compaction request dispatched for conversation: {}, messageCount={}",
                conversationId, history.size());
    }

    private List<Map<String, Object>> buildCompactHistory(Conversation conversation) {
        List<Map<String, Object>> history = new ArrayList<>();
        for (Message msg : conversation.getMessages()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", msg.getRole().name());
            entry.put("content", msg.getContent());
            if (msg.getFileUrl() != null) {
                entry.put("fileUrl", msg.getFileUrl());
            }
            history.add(entry);
        }
        return history;
    }

    /**
     * Applies a completed compaction result from the AI service.
     * 应用 AI 服务返回的压缩结果。
     */
    @Transactional(timeout = 30)
    public boolean applyCompactionResult(String conversationId, String requestId, String summary,
                                         int throughSequence, int promptTokens) {
        Conversation conversation = conversationRepository.findById(UUID.fromString(conversationId))
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        if (!conversation.completeCompaction(UUID.fromString(requestId), summary, throughSequence, promptTokens)) {
            log.info("Ignoring duplicate or stale compaction result: conversation={}, requestId={}",
                    conversationId, requestId);
            return false;
        }
        conversationRepository.save(conversation);

        log.info("Compaction applied for conversation: {}, throughSequence={}, contextTokens={}",
                conversationId, throughSequence, promptTokens);
        return true;
    }

    @Transactional(timeout = 30)
    public boolean failCompaction(String conversationId, String requestId) {
        Conversation conversation = conversationRepository.findById(UUID.fromString(conversationId))
                .orElseThrow(() -> new ConversationException("conversation.not.found"));
        if (!conversation.failCompaction(UUID.fromString(requestId))) {
            log.info("Ignoring duplicate or stale compaction failure: conversation={}, requestId={}",
                    conversationId, requestId);
            return false;
        }
        conversationRepository.save(conversation);
        return true;
    }
}
