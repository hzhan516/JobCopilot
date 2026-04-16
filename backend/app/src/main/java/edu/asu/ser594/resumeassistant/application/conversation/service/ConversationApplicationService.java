package edu.asu.ser594.resumeassistant.application.conversation.service;

import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.exception.ConversationException;
import edu.asu.ser594.resumeassistant.domain.conversation.repository.ConversationRepository;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ConversationRequestCommand;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final FileStorageService fileStorageService;

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
     * 发送消息：保存用户消息并异步投递 AI 请求
     * Send message: persist user message and publish async AI request
     */
    @Transactional
    public Conversation sendMessage(SendMessageCommand command) {
        log.info("Sending message to conversation: {}", command.conversationId());
        Conversation conversation = getConversationWithOwnershipCheck(command.conversationId(), command.userId());
        
        conversation.addMessage(command.role(), command.content());
        Conversation saved = conversationRepository.save(conversation);

        // 构造并发送 MQ 请求 / Build and send MQ request
        List<Map<String, Object>> history = buildMessageHistory(conversation);
        ConversationRequestCommand mqCommand = new ConversationRequestCommand(
            conversation.getId().toString(),
            command.userId().toString(),
            history,
            command.content(),
            command.fileUrls() != null ? command.fileUrls() : new ArrayList<>(),
            conversation.getResumeVersionId() != null ? conversation.getResumeVersionId().toString() : null
        );
        aiMessagePublisherPort.sendConversationRequest(mqCommand);
        log.info("Published conversation request to MQ for conversation: {}", conversation.getId());

        return saved;
    }

    /**
     * 保存 AI 回复消息
     * Save AI reply message
     */
    @Transactional
    public void saveAiReply(UUID conversationId, String content, String fileUrl) {
        log.info("Saving AI reply for conversation: {}", conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationException(ConversationException.ErrorType.NOT_FOUND, "Conversation not found"));
        conversation.addMessage(MessageRole.ASSISTANT, content, fileUrl);
        conversationRepository.save(conversation);
        log.info("AI reply saved for conversation: {}", conversationId);
    }

    /**
     * 上传对话附件到 MinIO 并返回访问 URL
     * Upload conversation attachment to MinIO and return access URL
     */
    @Transactional
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream, long size, String contentType, String fileName) {
        log.info("Uploading attachment for conversation: {}", conversationId);
        Conversation conversation = getConversationWithOwnershipCheck(conversationId, userId);

        String objectKey = "conversations/" + conversationId + "/" + UUID.randomUUID() + "_" + fileName;
        fileStorageService.upload(objectKey, inputStream, size, contentType);

        String fileUrl = fileStorageService.generatePresignedUrl(objectKey, java.time.Duration.ofDays(7));
        log.info("Attachment uploaded for conversation: {}, url: {}", conversationId, fileUrl);
        return fileUrl;
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

    /**
     * 构建消息历史记录（供 AI 服务使用）
     * Build message history for AI service
     */
    private List<Map<String, Object>> buildMessageHistory(Conversation conversation) {
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
}
