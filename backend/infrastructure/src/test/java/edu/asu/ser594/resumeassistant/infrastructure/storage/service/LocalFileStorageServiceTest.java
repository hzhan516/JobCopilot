package edu.asu.ser594.resumeassistant.infrastructure.storage.service;

import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.infrastructure.storage.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileStorageService 单元测试
 * LocalFileStorageService Unit Tests
 * <p>
 * 测试本地文件存储服务实现：
 * Tests the local file storage service implementation:
 * - 文件上传
 * - File upload
 * - 文件下载
 * - File download
 * - 文件删除
 * - File deletion
 * - 路径处理
 * - Path handling
 */
@DisplayName("Local File Storage Service Tests")
class LocalFileStorageServiceTest {

    private static final String TEST_FILE_PATH = "test/file.txt";
    private static final String TEST_CONTENT = "Hello, World!";
    @TempDir
    Path tempDir;
    private LocalFileStorageService storageService;
    private StorageProperties storageProperties;

    private static org.assertj.core.api.ThrowableTypeAssert<Throwable> assertThatNoExceptionThrown(ThrowableRunnable runnable) {
        try {
            runnable.run();
            return null; // Success
            // 成功
        } catch (Exception e) {
            throw new AssertionError("Expected no exception but got: " + e.getMessage(), e);
        }
    }

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.getLocal().setBasePath(tempDir.toString());
        storageProperties.getLocal().setResumePath("");
        storageProperties.getLocal().setDateSubdirectory(false);
        storageService = new LocalFileStorageService(storageProperties);
        storageService.init();
    }

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() throws Exception {
        // 给定
        // Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // 当
        // When
        storageService.upload(TEST_FILE_PATH, inputStream, TEST_CONTENT.length(), "text/plain");

        // 然后
        // Then
        Path uploadedFile = tempDir.resolve(TEST_FILE_PATH);
        assertThat(uploadedFile).exists();
        assertThat(Files.readString(uploadedFile)).isEqualTo(TEST_CONTENT);
    }

    // ==================== 上传测试 ====================
    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should create parent directories when uploading")
    void shouldCreateParentDirectoriesWhenUploading() throws Exception {
        // 给定
        // Given
        String nestedPath = "nested/deeply/file.txt";
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // 当
        // When
        storageService.upload(nestedPath, inputStream, TEST_CONTENT.length(), "text/plain");

        // 然后
        // Then
        Path uploadedFile = tempDir.resolve(nestedPath);
        assertThat(uploadedFile.getParent()).exists();
        assertThat(uploadedFile).exists();
    }

    @Test
    @DisplayName("Should throw exception when uploading null input stream")
    void shouldThrowExceptionWhenUploadingNullInputStream() {
        // 当与然后
        // When & Then
        assertThatThrownBy(() ->
                storageService.upload(TEST_FILE_PATH, null, 0, "text/plain")
        ).isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Should download existing file")
    void shouldDownloadExistingFile() throws IOException {
        // 给定
        // Given
        Path filePath = tempDir.resolve(TEST_FILE_PATH);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, TEST_CONTENT);

        // 当
        // When
        Optional<InputStream> result = storageService.download(TEST_FILE_PATH);

        // 然后
        // Then
        assertThat(result).isPresent();
        assertThat(new String(result.get().readAllBytes())).isEqualTo(TEST_CONTENT);
    }

    // ==================== 下载测试 ====================
    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should return empty when downloading non-existent file")
    void shouldReturnEmptyWhenDownloadingNonExistentFile() {
        // 当
        // When
        Optional<InputStream> result = storageService.download("non/existent/file.txt");

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when downloading directory")
    void shouldReturnEmptyWhenDownloadingDirectory() throws IOException {
        // 给定
        // Given
        Path dirPath = tempDir.resolve("testdir");
        Files.createDirectories(dirPath);

        // 当
        // When
        Optional<InputStream> result = storageService.download("testdir");

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete existing file")
    void shouldDeleteExistingFile() throws IOException {
        // 给定
        // Given
        Path filePath = tempDir.resolve(TEST_FILE_PATH);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, TEST_CONTENT);

        // 当
        // When
        storageService.delete(TEST_FILE_PATH);

        // 然后
        // Then
        assertThat(filePath).doesNotExist();
    }

    // ==================== 删除测试 ====================
    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should handle deleting non-existent file gracefully")
    void shouldHandleDeletingNonExistentFileGracefully() {
        // 当
        // When
        try {
            storageService.delete("non/existent/file.txt");
            // 然后 - 未抛出异常
            // Then - no exception thrown
            assertThat(true).isTrue();
        } catch (Exception e) {
            assertThat(false).isTrue(); // Should not reach here
            // 不应到达此处
        }
    }

    @Test
    @DisplayName("Should handle complete upload-download-delete cycle")
    void shouldHandleCompleteUploadDownloadDeleteCycle() throws IOException {
        // 给定
        // Given
        String path = "cycle/test.txt";
        InputStream uploadStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // 当 - 上传
        // When - Upload
        storageService.upload(path, uploadStream, TEST_CONTENT.length(), "text/plain");

        // 然后 - 验证上传
        // Then - Verify upload
        assertThat(tempDir.resolve(path)).exists();

        // 当 - 下载
        // When - Download
        Optional<InputStream> downloadStream = storageService.download(path);

        // 然后 - 验证下载
        // Then - Verify download
        assertThat(downloadStream).isPresent();
        assertThat(new String(downloadStream.get().readAllBytes())).isEqualTo(TEST_CONTENT);

        // 当 - 删除
        // When - Delete
        storageService.delete(path);

        // 然后 - 验证删除
        // Then - Verify delete
        assertThat(tempDir.resolve(path)).doesNotExist();
    }

    // ==================== 集成测试 ====================
    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle binary content")
    void shouldHandleBinaryContent() throws IOException {
        // 给定
        // Given
        byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, 0x03, (byte) 0xFF};
        InputStream inputStream = new ByteArrayInputStream(binaryContent);
        String path = "binary/file.bin";

        // 当
        // When
        storageService.upload(path, inputStream, binaryContent.length, "application/octet-stream");

        // 然后
        // Then
        Optional<InputStream> downloaded = storageService.download(path);
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get().readAllBytes()).isEqualTo(binaryContent);
    }

    @Test
    @DisplayName("Should handle empty file")
    void shouldHandleEmptyFile() throws IOException {
        // 给定
        // Given
        String path = "empty/file.txt";
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // 当
        // When
        storageService.upload(path, inputStream, 0, "text/plain");

        // 然后
        // Then
        Optional<InputStream> downloaded = storageService.download(path);
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get().readAllBytes()).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters in path")
    void shouldHandleSpecialCharactersInPath() throws IOException {
        // 给定
        // Given
        String path = "special/file with spaces and-_.txt";
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // 当
        // When
        storageService.upload(path, inputStream, TEST_CONTENT.length(), "text/plain");

        // 然后
        // Then
        assertThat(tempDir.resolve(path)).exists();
    }

    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Exception;
    }
}
