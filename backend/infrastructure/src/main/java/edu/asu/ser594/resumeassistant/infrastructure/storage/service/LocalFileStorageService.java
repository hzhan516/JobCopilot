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
 * File storage implementation using the local filesystem.
 * Activated when storage.type=local, suitable for single-node deployments or development environments.
 * 基于本地文件系统的存储实现，在 storage.type=local 时激活，适用于单节点部署或开发环境
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
        // Local storage cannot generate real presigned URLs; return a time-limited proxy URL instead.
        // In production behind a CDN or nginx, configure urlPrefix to emit valid external links.
        // 本地存储无法生成真正的预签名 URL，返回带过期时间的代理地址；生产环境配置 urlPrefix 可发出有效外链
        StorageProperties.Local local = storageProperties.getLocal();
        String urlPrefix = local.getUrlPrefix();

        if (urlPrefix == null || urlPrefix.isEmpty()) {
            Instant expiryTime = Instant.now().plus(expiration);
            return String.format("/api/storage/download?key=%s&expiry=%s", objectKey, expiryTime.toEpochMilli());
        }

        Instant expiryTime = Instant.now().plus(expiration);
        return String.format("%s/%s?expiry=%s", urlPrefix, objectKey, expiryTime.toEpochMilli());
    }

    private Path resolvePath(String objectKey) {
        // Sanitize objectKey to prevent directory traversal attacks
        // 清理 objectKey 防止目录遍历攻击
        String sanitizedKey = objectKey.replace("..", "")
                .replaceAll("[:*?\"<>|]", "_");

        StorageProperties.Local local = storageProperties.getLocal();
        if (local.isDateSubdirectory()) {
            java.time.LocalDate now = java.time.LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
            return basePath.resolve(datePath).resolve(sanitizedKey);
        }

        return basePath.resolve(sanitizedKey);
    }

    public Path getAbsolutePath(String objectKey) {
        return resolvePath(objectKey);
    }
}
