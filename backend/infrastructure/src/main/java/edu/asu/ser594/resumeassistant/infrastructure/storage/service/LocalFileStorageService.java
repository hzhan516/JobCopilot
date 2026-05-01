package edu.asu.ser594.resumeassistant.infrastructure.storage.service;

import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.infrastructure.storage.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 本地文件存储服务实现
 * Local file storage service implementation - activated when storage.type=local
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local")
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;

    private Path basePath;

    @PostConstruct
    public void init() {
        StorageProperties.Local local = storageProperties.getLocal();
        this.basePath = Paths.get(local.getBasePath(), local.getResumePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
            log.info("Initialized local file storage at: {}", basePath);
        } catch (IOException e) {
            log.error("Failed to create local storage directory: {}", basePath, e);
            throw new StorageException("storage.init.failed", e);
        }
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long size, String contentType) {
        if (inputStream == null) {
            throw new StorageException("storage.upload.failed", new IllegalArgumentException("inputStream cannot be null"));
        }
        try {
            Path targetPath = resolvePath(objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded to local storage: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to upload file to local storage: {}", objectKey, e);
            throw new StorageException("storage.upload.failed", e);
        }
    }

    @Override
    public Optional<InputStream> download(String objectKey) {
        try {
            Path filePath = resolvePath(objectKey);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return Optional.empty();
            }
            return Optional.of(Files.newInputStream(filePath));
        } catch (IOException e) {
            log.error("Failed to download file from local storage: {}", objectKey, e);
            throw new StorageException("storage.download.failed", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path filePath = resolvePath(objectKey);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted from local storage: {}", filePath);
            } else {
                log.warn("File not found in local storage: {}", objectKey);
            }
        } catch (IOException e) {
            log.error("Failed to delete file from local storage: {}", objectKey, e);
            throw new StorageException("storage.delete.failed", e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path filePath = resolvePath(objectKey);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    public String generatePresignedUrl(String objectKey, Duration expiration) {
        // 对于本地存储，我们生成一个带有过期时间戳的临时访问 URL
        // For local storage, we generate a temporary access URL with expiration timestamp
        // 在生产环境中，您可能需要实现基于令牌的访问机制
        // In production, you might want to implement a token-based access mechanism
        StorageProperties.Local local = storageProperties.getLocal();
        String urlPrefix = local.getUrlPrefix();

        if (urlPrefix == null || urlPrefix.isEmpty()) {
            // 如果未配置 URL 前缀，则返回带有过期信息的占位符
            // If no URL prefix configured, return a placeholder with expiration info
            Instant expiryTime = Instant.now().plus(expiration);
            return String.format("/api/storage/download?key=%s&expiry=%s", objectKey, expiryTime.toEpochMilli());
        }

        // 如果配置了 URL 前缀（例如，位于 CDN 或 nginx 之后），则使用它
        // If URL prefix is configured (e.g., behind a CDN or nginx), use it
        Instant expiryTime = Instant.now().plus(expiration);
        return String.format("%s/%s?expiry=%s", urlPrefix, objectKey, expiryTime.toEpochMilli());
    }

    /**
     * 解析文件路径
     * Resolve file path based on configuration
     */
    private Path resolvePath(String objectKey) {
        // 清理 objectKey 以防止目录遍历
        // Sanitize objectKey to prevent directory traversal
        String sanitizedKey = objectKey.replaceAll("\\.\\.", "")
                .replaceAll("[:*?\"<>|]", "_");

        StorageProperties.Local local = storageProperties.getLocal();
        if (local.isDateSubdirectory()) {
            // 创建基于日期的子目录：basePath/YYYY/MM/DD/objectKey
            // Create date-based subdirectory: basePath/YYYY/MM/DD/objectKey
            java.time.LocalDate now = java.time.LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
            return basePath.resolve(datePath).resolve(sanitizedKey);
        }

        return basePath.resolve(sanitizedKey);
    }

    /**
     * 获取文件的绝对路径（用于调试或其他用途）
     * Get absolute path of a file
     */
    public Path getAbsolutePath(String objectKey) {
        return resolvePath(objectKey);
    }
}
