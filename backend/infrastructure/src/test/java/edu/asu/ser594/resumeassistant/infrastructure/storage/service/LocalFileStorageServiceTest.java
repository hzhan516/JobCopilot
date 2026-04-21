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
 * LocalFileStorageService Unit Tests
 * 
 * Tests the local file storage service implementation:
 * - File upload
 * - File download
 * - File deletion
 * - Path handling
 */
@DisplayName("Local File Storage Service Tests")
class LocalFileStorageServiceTest {

    @FunctionalInterface
    interface ThrowableRunnable {
        void run() throws Exception;
    }

    private static org.assertj.core.api.ThrowableTypeAssert<Throwable> assertThatNoExceptionThrown(ThrowableRunnable runnable) {
        try {
            runnable.run();
            return null; // Success
        } catch (Exception e) {
            throw new AssertionError("Expected no exception but got: " + e.getMessage(), e);
        }
    }

    private static final String TEST_FILE_PATH = "test/file.txt";
    private static final String TEST_CONTENT = "Hello, World!";

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;
    private StorageProperties storageProperties;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.getLocal().setBasePath(tempDir.toString());
        storageProperties.getLocal().setResumePath("");
        storageProperties.getLocal().setDateSubdirectory(false);
        storageService = new LocalFileStorageService(storageProperties);
        storageService.init();
    }

    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should upload file successfully")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // When
        storageService.upload(TEST_FILE_PATH, inputStream, TEST_CONTENT.length(), "text/plain");

        // Then
        Path uploadedFile = tempDir.resolve(TEST_FILE_PATH);
        assertThat(uploadedFile).exists();
        assertThat(Files.readString(uploadedFile)).isEqualTo(TEST_CONTENT);
    }

    @Test
    @DisplayName("Should create parent directories when uploading")
    void shouldCreateParentDirectoriesWhenUploading() throws Exception {
        // Given
        String nestedPath = "nested/deeply/file.txt";
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // When
        storageService.upload(nestedPath, inputStream, TEST_CONTENT.length(), "text/plain");

        // Then
        Path uploadedFile = tempDir.resolve(nestedPath);
        assertThat(uploadedFile.getParent()).exists();
        assertThat(uploadedFile).exists();
    }

    @Test
    @DisplayName("Should throw exception when uploading null input stream")
    void shouldThrowExceptionWhenUploadingNullInputStream() {
        // When & Then
        assertThatThrownBy(() ->
                storageService.upload(TEST_FILE_PATH, null, 0, "text/plain")
        ).isInstanceOf(StorageException.class);
    }

    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should download existing file")
    void shouldDownloadExistingFile() throws IOException {
        // Given
        Path filePath = tempDir.resolve(TEST_FILE_PATH);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, TEST_CONTENT);

        // When
        Optional<InputStream> result = storageService.download(TEST_FILE_PATH);

        // Then
        assertThat(result).isPresent();
        assertThat(new String(result.get().readAllBytes())).isEqualTo(TEST_CONTENT);
    }

    @Test
    @DisplayName("Should return empty when downloading non-existent file")
    void shouldReturnEmptyWhenDownloadingNonExistentFile() {
        // When
        Optional<InputStream> result = storageService.download("non/existent/file.txt");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when downloading directory")
    void shouldReturnEmptyWhenDownloadingDirectory() throws IOException {
        // Given
        Path dirPath = tempDir.resolve("testdir");
        Files.createDirectories(dirPath);

        // When
        Optional<InputStream> result = storageService.download("testdir");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should delete existing file")
    void shouldDeleteExistingFile() throws IOException {
        // Given
        Path filePath = tempDir.resolve(TEST_FILE_PATH);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, TEST_CONTENT);

        // When
        storageService.delete(TEST_FILE_PATH);

        // Then
        assertThat(filePath).doesNotExist();
    }

    @Test
    @DisplayName("Should handle deleting non-existent file gracefully")
    void shouldHandleDeletingNonExistentFileGracefully() {
        // When
        try {
            storageService.delete("non/existent/file.txt");
            // Then - no exception thrown
            assertThat(true).isTrue();
        } catch (Exception e) {
            assertThat(false).isTrue(); // Should not reach here
        }
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle complete upload-download-delete cycle")
    void shouldHandleCompleteUploadDownloadDeleteCycle() throws IOException {
        // Given
        String path = "cycle/test.txt";
        InputStream uploadStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // When - Upload
        storageService.upload(path, uploadStream, TEST_CONTENT.length(), "text/plain");

        // Then - Verify upload
        assertThat(tempDir.resolve(path)).exists();

        // When - Download
        Optional<InputStream> downloadStream = storageService.download(path);

        // Then - Verify download
        assertThat(downloadStream).isPresent();
        assertThat(new String(downloadStream.get().readAllBytes())).isEqualTo(TEST_CONTENT);

        // When - Delete
        storageService.delete(path);

        // Then - Verify delete
        assertThat(tempDir.resolve(path)).doesNotExist();
    }

    @Test
    @DisplayName("Should handle binary content")
    void shouldHandleBinaryContent() throws IOException {
        // Given
        byte[] binaryContent = new byte[]{0x00, 0x01, 0x02, 0x03, (byte) 0xFF};
        InputStream inputStream = new ByteArrayInputStream(binaryContent);
        String path = "binary/file.bin";

        // When
        storageService.upload(path, inputStream, binaryContent.length, "application/octet-stream");

        // Then
        Optional<InputStream> downloaded = storageService.download(path);
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get().readAllBytes()).isEqualTo(binaryContent);
    }

    @Test
    @DisplayName("Should handle empty file")
    void shouldHandleEmptyFile() throws IOException {
        // Given
        String path = "empty/file.txt";
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // When
        storageService.upload(path, inputStream, 0, "text/plain");

        // Then
        Optional<InputStream> downloaded = storageService.download(path);
        assertThat(downloaded).isPresent();
        assertThat(downloaded.get().readAllBytes()).isEmpty();
    }

    @Test
    @DisplayName("Should handle special characters in path")
    void shouldHandleSpecialCharactersInPath() throws IOException {
        // Given
        String path = "special/file with spaces and-_.txt";
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT.getBytes());

        // When
        storageService.upload(path, inputStream, TEST_CONTENT.length(), "text/plain");

        // Then
        assertThat(tempDir.resolve(path)).exists();
    }
}
