package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import io.jobcopilot.resumeassistant.infrastructure.storage.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Handles attachment uploads for conversations: security validation, storage, and presigned URL generation.
 * 处理对话附件上传：安全校验、存储和预签名 URL 生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAttachmentService {

    private final ConversationRepository conversationRepository;
    private final ConversationLifecycleService lifecycleService;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Transactional(timeout = 30)
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream,
                                    long size, String contentType, String fileName) {
        log.info("Uploading attachment for conversation: {}", conversationId);
        lifecycleService.getConversationWithOwnershipCheck(conversationId, userId);

        if (fileName == null || fileName.isBlank()) {
            throw new ConversationException("attachment.filename.required");
        }
        String safeFileName = fileName.replaceAll("[\\\\/]", "_")
                .replaceAll("\\.\\.+", "_")
                .replaceAll("[\\x00-\\x1f\\x7f]", "_");
        String objectKey = "conversations/" + conversationId + "/" + UUID.randomUUID() + "_" + safeFileName;
        fileStorageService.upload(objectKey, inputStream, size, contentType);

        int expiryHours = storageProperties.getPresignedUrlExpirationHours();
        String fileUrl = fileStorageService.generatePresignedUrl(objectKey, Duration.ofHours(expiryHours));
        log.info("Attachment uploaded for conversation: {}, url: {}", conversationId, fileUrl);
        return fileUrl;
    }
}
