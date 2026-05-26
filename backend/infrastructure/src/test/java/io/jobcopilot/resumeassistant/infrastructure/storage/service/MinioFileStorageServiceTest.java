package io.jobcopilot.resumeassistant.infrastructure.storage.service;

import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import io.jobcopilot.resumeassistant.infrastructure.storage.config.StorageProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MinIO 文件存储服务单元测试
 * MinIO File Storage Service Unit Tests
 *
 * 测试对象存储核心操作：
 * Tests core object storage operations:
 * - 桶初始化 / Bucket initialization
 * - 文件上传 / File upload
 * - 文件下载 / File download
 * - 文件删除 / File deletion
 * - 存在性检查 / Existence check
 * - 预签名 URL / Presigned URL
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MinIO File Storage Service Tests")
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private MinioFileStorageService storageService;

    private static final String BUCKET_NAME = "resume-bucket";
    private static final String OBJECT_KEY = "resumes/test.pdf";
    private static final String TEST_CONTENT = "Hello, MinIO!";

    @BeforeEach
    void setUp() {
        StorageProperties.Minio minio = new StorageProperties.Minio();
        minio.setBucketName(BUCKET_NAME);
        minio.setAccessKey("test-access-key");
        minio.setSecretKey("test-secret-key");
        when(storageProperties.getMinio()).thenReturn(minio);
    }

    // ==================== 桶初始化 ====================
    // ==================== Bucket Init ====================

    @Test
    @DisplayName("Should create bucket when it does not exist")
    void shouldCreateBucketWhenNotExists() throws Exception {
        // 给定 / Given
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        doNothing().when(minioClient).makeBucket(any(MakeBucketArgs.class));

        // 当 / When
        storageService.init();

        // 那么 / Then
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("Should skip bucket creation when it already exists")
    void shouldSkipBucketCreationWhenExists() throws Exception {
        // 给定 / Given
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        // 当 / When
        storageService.init();

        // 那么 / Then
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("Should throw on missing credentials during init")
    void shouldThrowOnMissingCredentialsDuringInit() {
        // 给定 / Given
        StorageProperties.Minio minio = new StorageProperties.Minio();
        minio.setBucketName(BUCKET_NAME);
        minio.setAccessKey("");
        minio.setSecretKey("");
        when(storageProperties.getMinio()).thenReturn(minio);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.init())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MinIO credentials are not configured");
    }

    @Test
    @DisplayName("Should handle init exception gracefully")
    void shouldHandleInitExceptionGracefully() throws Exception {
        // 给定 / Given
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // 当 / When — should not throw
        assertThatNoException().isThrownBy(() -> storageService.init());
    }

    // ==================== 上传 ====================
    // ==================== Upload ====================

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() throws Exception {
        // 给定 / Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenReturn(mock(ObjectWriteResponse.class));

        // 当 / When
        storageService.upload(OBJECT_KEY, inputStream, TEST_CONTENT.length(), "application/pdf");

        // 那么 / Then
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("Should throw StorageException on upload failure")
    void shouldThrowStorageExceptionOnUploadFailure() throws Exception {
        // 给定 / Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("Network error"));

        // 当&那么 / When & Then
        assertThatThrownBy(() ->
                storageService.upload(OBJECT_KEY, inputStream, TEST_CONTENT.length(), "text/plain")
        ).isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.upload.failed");
    }

    // ==================== 下载 ====================
    // ==================== Download ====================

    @Test
    @DisplayName("Should download existing file")
    void shouldDownloadExistingFile() throws Exception {
        // 给定 / Given
        InputStream mockStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockStream);

        // 当 / When
        Optional<InputStream> result = storageService.download(OBJECT_KEY);

        // 那么 / Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should return empty when file not found (404)")
    void shouldReturnEmptyWhenFileNotFound() throws Exception {
        // 给定 / Given
        ErrorResponseException notFound = mock(ErrorResponseException.class);
        when(notFound.response()).thenReturn(mock(okhttp3.Response.class));
        when(notFound.response().code()).thenReturn(404);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(notFound);

        // 当 / When
        Optional<InputStream> result = storageService.download(OBJECT_KEY);

        // 那么 / Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw StorageException on download error")
    void shouldThrowStorageExceptionOnDownloadError() throws Exception {
        // 给定 / Given
        ErrorResponseException error = mock(ErrorResponseException.class);
        when(error.response()).thenReturn(mock(okhttp3.Response.class));
        when(error.response().code()).thenReturn(500);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(error);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.download(OBJECT_KEY))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.download.failed");
    }

    @Test
    @DisplayName("Should throw StorageException on generic download exception")
    void shouldThrowStorageExceptionOnGenericDownloadException() throws Exception {
        // 给定 / Given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("Unexpected"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.download(OBJECT_KEY))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.download.failed");
    }

    // ==================== 删除 ====================
    // ==================== Delete ====================

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() throws Exception {
        // 给定 / Given
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        // 当 / When
        assertThatNoException().isThrownBy(() -> storageService.delete(OBJECT_KEY));

        // 那么 / Then
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("Should throw StorageException on delete failure")
    void shouldThrowStorageExceptionOnDeleteFailure() throws Exception {
        // 给定 / Given
        doThrow(new RuntimeException("Access denied"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.delete(OBJECT_KEY))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.delete.failed");
    }

    // ==================== 存在性检查 ====================
    // ==================== Existence Check ====================

    @Test
    @DisplayName("Should return true when file exists")
    void shouldReturnTrueWhenFileExists() throws Exception {
        // 给定 / Given
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenReturn(mock(StatObjectResponse.class));

        // 当 / When
        boolean exists = storageService.exists(OBJECT_KEY);

        // 那么 / Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when file not found (404)")
    void shouldReturnFalseWhenFileNotFound() throws Exception {
        // 给定 / Given
        ErrorResponseException notFound = mock(ErrorResponseException.class);
        when(notFound.response()).thenReturn(mock(okhttp3.Response.class));
        when(notFound.response().code()).thenReturn(404);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(notFound);

        // 当 / When
        boolean exists = storageService.exists(OBJECT_KEY);

        // 那么 / Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should throw StorageException on existence check error")
    void shouldThrowStorageExceptionOnExistenceCheckError() throws Exception {
        // 给定 / Given
        ErrorResponseException error = mock(ErrorResponseException.class);
        when(error.response()).thenReturn(mock(okhttp3.Response.class));
        when(error.response().code()).thenReturn(500);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(error);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.exists(OBJECT_KEY))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.check.failed");
    }

    // ==================== 预签名 URL ====================
    // ==================== Presigned URL ====================

    @Test
    @DisplayName("Should generate presigned URL successfully")
    void shouldGeneratePresignedUrlSuccessfully() throws Exception {
        // 给定 / Given
        String expectedUrl = "https://minio.example.com/resume-bucket/resumes/test.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);

        // 当 / When
        String url = storageService.generatePresignedUrl(OBJECT_KEY, Duration.ofHours(1));

        // 那么 / Then
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Should throw StorageException on socket timeout during upload / 上传时Socket超时异常应抛出StorageException")
    void shouldThrowStorageExceptionOnSocketTimeoutDuringUpload() throws Exception {
        // 给定 / Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new SocketTimeoutException("Connection timed out"));

        // 当&那么 / When & Then
        assertThatThrownBy(() ->
                storageService.upload(OBJECT_KEY, inputStream, TEST_CONTENT.length(), "text/plain")
        ).isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.upload.failed");
    }

    @Test
    @DisplayName("Should throw StorageException on connect exception during download / 下载时连接异常应抛出StorageException")
    void shouldThrowStorageExceptionOnConnectExceptionDuringDownload() throws Exception {
        // 给定 / Given
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new ConnectException("Connection refused"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.download(OBJECT_KEY))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.download.failed");
    }

    @Test
    @DisplayName("Should throw StorageException on socket timeout during presign / 预签名URL时Socket超时异常应抛出StorageException")
    void shouldThrowStorageExceptionOnSocketTimeoutDuringPresign() throws Exception {
        // 给定 / Given
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new SocketTimeoutException("Read timed out"));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> storageService.generatePresignedUrl(OBJECT_KEY, Duration.ofMinutes(30)))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("storage.presign.failed");
    }
}
