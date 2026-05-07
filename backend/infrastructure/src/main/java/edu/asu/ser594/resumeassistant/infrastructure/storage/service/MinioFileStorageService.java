package edu.asu.ser594.resumeassistant.infrastructure.storage.service;

import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.infrastructure.storage.config.StorageProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * MinIO 文件存储服务实现
 * MinIO file storage service implementation - activated when storage.type=minio
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio", matchIfMissing = true)
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void init() {
        String accessKey = storageProperties.getMinio().getAccessKey();
        String secretKey = storageProperties.getMinio().getSecretKey();
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "MinIO credentials are not configured. "
                            + "Please set 'storage.minio.access-key' and 'storage.minio.secret-key'. / "
                            + "MinIO 凭证未配置。请设置 'storage.minio.access-key' 和 'storage.minio.secret-key'。"
            );
        }

        String bucketName = storageProperties.getMinio().getBucketName();
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
        }
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            String bucketName = storageProperties.getMinio().getBucketName();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("File uploaded to MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", objectKey, e);
            throw new StorageException("storage.upload.failed", e);
        }
    }

    @Override
    public Optional<InputStream> download(String objectKey) {
        try {
            String bucketName = storageProperties.getMinio().getBucketName();
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return Optional.of(response);
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                return Optional.empty();
            }
            throw new StorageException("storage.download.failed", e);
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", objectKey, e);
            throw new StorageException("storage.download.failed", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            String bucketName = storageProperties.getMinio().getBucketName();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            log.info("File deleted from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectKey, e);
            throw new StorageException("storage.delete.failed", e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            String bucketName = storageProperties.getMinio().getBucketName();
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                return false;
            }
            throw new StorageException("storage.check.failed", e);
        } catch (Exception e) {
            log.error("Failed to check file existence in MinIO: {}", objectKey, e);
            throw new StorageException("storage.check.failed", e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey, Duration expiration) {
        try {
            String bucketName = storageProperties.getMinio().getBucketName();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry((int) expiration.getSeconds())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL from MinIO: {}", objectKey, e);
            throw new StorageException("storage.presign.failed", e);
        }
    }
}
