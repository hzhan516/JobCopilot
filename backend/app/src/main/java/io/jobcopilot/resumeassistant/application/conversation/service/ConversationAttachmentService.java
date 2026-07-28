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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Handles attachment uploads for conversations: security validation, storage, and presigned URL generation.
 * 处理对话附件上传：安全校验、存储和预签名 URL 生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAttachmentService {

    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt", "md");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "application/octet-stream");

    private final ConversationRepository conversationRepository;
    private final ConversationLifecycleService lifecycleService;
    private final FileStorageService fileStorageService;
    private final StorageProperties storageProperties;

    @Transactional(timeout = 30)
    public String uploadAttachment(UUID conversationId, UUID userId, InputStream inputStream,
                                    long size, String contentType, String fileName) {
        log.info("Uploading attachment for conversation: {}", conversationId);
        lifecycleService.getConversationWithOwnershipCheck(conversationId, userId);

        if (size <= 0 || size > MAX_ATTACHMENT_BYTES) {
            throw new ConversationException("attachment.size.invalid");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new ConversationException("attachment.filename.required");
        }
        String extension = extensionOf(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ConversationException("attachment.type.invalid");
        }
        String safeFileName = fileName.replaceAll("[\\\\/]", "_")
                .replaceAll("\\.\\.+", "_")
                .replaceAll("[\\x00-\\x1f\\x7f]", "_");
        String objectKey = "conversations/" + conversationId + "/" + UUID.randomUUID() + "_" + safeFileName;
        fileStorageService.upload(objectKey, inputStream, size, contentType);

        int expiryHours = storageProperties.getPresignedUrlExpirationHours();
        String fileUrl = fileStorageService.generatePresignedUrl(objectKey, Duration.ofHours(expiryHours));
        log.info("Attachment uploaded for conversation: {}, size={}, contentType={}",
                conversationId, size, contentType);
        return fileUrl;
    }

    public List<String> validateReferences(UUID conversationId, UUID userId, List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return List.of();
        }
        lifecycleService.getConversationWithOwnershipCheck(conversationId, userId);
        if (fileUrls.size() > 3) {
            throw new ConversationException("attachment.count.invalid");
        }
        String marker = "conversations/" + conversationId + "/";
        return fileUrls.stream().map(String::trim).peek(url -> {
            String decoded;
            try {
                decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                throw new ConversationException("attachment.reference.invalid");
            }
            if (url.length() > 2048 || !decoded.contains(marker) || !isManagedStorageReference(url)) {
                throw new ConversationException("attachment.reference.invalid");
            }
        }).toList();
    }

    private boolean isManagedStorageReference(String reference) {
        if (reference.startsWith("/api/storage/download?")) {
            return "local".equalsIgnoreCase(storageProperties.getType());
        }
        try {
            URI uri = URI.create(reference);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return false;
            }
            String expected = switch (storageProperties.getType().toLowerCase(Locale.ROOT)) {
                case "minio" -> storageProperties.getMinio().getEndpoint();
                case "s3" -> storageProperties.getS3().getEndpoint();
                case "oss" -> storageProperties.getOss().getCdnDomain().isBlank()
                        ? storageProperties.getOss().getEndpoint() : storageProperties.getOss().getCdnDomain();
                case "local" -> storageProperties.getLocal().getUrlPrefix();
                default -> "";
            };
            if (expected == null || expected.isBlank()) {
                return false;
            }
            URI expectedUri = URI.create(expected.contains("://") ? expected : "https://" + expected);
            return uri.getHost() != null && uri.getHost().equalsIgnoreCase(expectedUri.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
