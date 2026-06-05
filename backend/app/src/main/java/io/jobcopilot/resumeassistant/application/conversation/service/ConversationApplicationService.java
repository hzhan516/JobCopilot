package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.application.conversation.command.CreateConversationCommand;
import io.jobcopilot.resumeassistant.application.conversation.command.SendMessageCommand;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

/**
 * Thin orchestrator that delegates to focused conversation sub-services.
 * Keeps the facade stable while the internal services evolve independently.
 * 薄层编排器，将操作委托给聚焦的对话子服务。保持 Facade 稳定，内部服务可独立演进。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationApplicationService {

    private final ConversationLifecycleService lifecycleService;
    private final ConversationMessageService messageService;
    private final ConversationAttachmentService attachmentService;

    @Transactional(timeout = 30)
    public Conversation createConversation(CreateConversationCommand command) {
        return lifecycleService.createConversation(command);
    }

    @Transactional(timeout = 30)
    public Conversation sendMessage(SendMessageCommand command) {
        return messageService.sendMessage(command);
    }

    @Transactional(timeout = 30)
    public void saveAiReply(UUID conversationId, String content, String fileUrl) {
        messageService.saveAiReply(conversationId, content, fileUrl, null);
    }

    @Transactional(timeout = 30)
    public void saveAiReply(UUID conversationId, String content, String fileUrl, String aiOptimizedMarkdown) {
        messageService.saveAiReply(conversationId, content, fileUrl, aiOptimizedMarkdown);
    }

    @Transactional(timeout = 30)
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream,
                                    long size, String contentType, String fileName) {
        return attachmentService.uploadAttachment(conversationId, userId, inputStream, size, contentType, fileName);
    }

    @Transactional(timeout = 30)
    public void closeConversation(UUID conversationId, UUID userId) {
        lifecycleService.closeConversation(conversationId, userId);
    }

    @Transactional(timeout = 30)
    public void deleteConversation(UUID conversationId, UUID userId) {
        lifecycleService.deleteConversation(conversationId, userId);
    }

    public void completeAiReply(UUID conversationId, String content) {
        messageService.completeAiReply(conversationId, content);
    }

    public void failAiReply(UUID conversationId, String errorMessage) {
        messageService.failAiReply(conversationId, errorMessage);
    }
}
