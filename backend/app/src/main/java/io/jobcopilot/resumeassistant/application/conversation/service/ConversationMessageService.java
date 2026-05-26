package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.api.conversation.port.ConversationStreamPort;
import io.jobcopilot.resumeassistant.application.conversation.command.SendMessageCommand;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles message-related operations within a conversation: sending user messages,
 * persisting AI replies, and managing the AI stream lifecycle.
 * 处理对话内的消息相关操作：发送用户消息、持久化 AI 回复、管理 AI 流生命周期。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationLifecycleService lifecycleService;
    private final ConversationContextService contextService;
    private final AiOptimizedResumeService aiOptimizedResumeService;
    private final ConversationStreamPort streamPort;

    @Transactional(timeout = 30)
    public Conversation sendMessage(SendMessageCommand command) {
        log.info("Sending message to conversation: {}", command.conversationId());
        Conversation conversation = lifecycleService.getConversationWithOwnershipCheck(
                command.conversationId(), command.userId());

        conversation.addMessage(command.role(), command.content());
        conversation.autoGenerateTitle(command.content());
        Conversation saved = conversationRepository.save(conversation);

        boolean isInit = saved.getMessages().stream()
                .noneMatch(m -> m.getRole() == MessageRole.ASSISTANT);
        contextService.deferConversationRequest(saved, command.content(), isInit);

        return saved;
    }

    @Transactional(timeout = 30)
    public void saveAiReply(UUID conversationId, String content, String fileUrl, String aiOptimizedMarkdown) {
        log.info("Saving AI reply for conversation: {}", conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationException("conversation.not.found"));

        String finalContent = content;
        if (aiOptimizedMarkdown != null && !aiOptimizedMarkdown.isBlank()
                && conversation.getResumeVersionId() != null) {
            UUID aiVersionId = aiOptimizedResumeService.saveOrUpdateAiOptimizedResume(conversation, aiOptimizedMarkdown);
            finalContent = content + "\n\n---\n\n" + aiOptimizedMarkdown;
            contextService.deferVectorGeneration(aiVersionId, aiOptimizedMarkdown);
        }

        conversation.addMessage(MessageRole.ASSISTANT, finalContent, fileUrl);
        conversationRepository.save(conversation);

        log.info("AI reply saved for conversation: {}", conversationId);
    }

    @Transactional(timeout = 30)
    public void saveAiReply(UUID conversationId, String content, String fileUrl) {
        saveAiReply(conversationId, content, fileUrl, null);
    }

    public void completeAiReply(UUID conversationId, String content) {
        streamPort.completeReply(conversationId.toString(), content);
        log.info("Completed AI stream reply for conversation: {}", conversationId);
    }

    public void failAiReply(UUID conversationId, String errorMessage) {
        streamPort.failReply(conversationId.toString(), errorMessage);
        log.warn("Failed AI stream reply for conversation: {}, error: {}", conversationId, errorMessage);
    }
}
