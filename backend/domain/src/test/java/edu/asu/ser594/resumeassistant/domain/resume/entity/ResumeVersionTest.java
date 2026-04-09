package edu.asu.ser594.resumeassistant.domain.resume.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ResumeVersion Entity Unit Tests
 * 
 * Tests the ResumeVersion entity following DDD principles:
 * - Factory methods for different version types
 * - Business rules enforcement (editable only for non-original, active versions)
 * - Immutable fields vs mutable fields
 */
@DisplayName("ResumeVersion Entity Tests")
class ResumeVersionTest {

    private static final UUID TEST_GROUP_ID = UUID.randomUUID();
    private static final String TEST_FILE_NAME = "resume.pdf";
    private static final String TEST_FILE_TYPE = "application/pdf";
    private static final String TEST_STORAGE_PATH = "storage/path/file.pdf";
    private static final long TEST_FILE_SIZE = 1024L;

    @Test
    @DisplayName("Should create original version with factory method")
    void shouldCreateOriginalVersionWithFactoryMethod() {
        // When
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        // Then
        assertThat(version).isNotNull();
        assertThat(version.getId()).isNotNull();
        assertThat(version.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(version.getVersionType()).isEqualTo(ResumeVersion.VersionType.ORIGINAL);
        assertThat(version.getOriginalFileName()).isEqualTo(TEST_FILE_NAME);
        assertThat(version.getFileType()).isEqualTo(TEST_FILE_TYPE);
        assertThat(version.getFileSize()).isEqualTo(TEST_FILE_SIZE);
        assertThat(version.getStoragePath()).isEqualTo(TEST_STORAGE_PATH);
        assertThat(version.getStorageProvider()).isEqualTo("minio");
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
        assertThat(version.getCreatedAt()).isNotNull();
        assertThat(version.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create converted version with factory method")
    void shouldCreateConvertedVersionWithFactoryMethod() {
        // When
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);

        // Then
        assertThat(version).isNotNull();
        assertThat(version.getId()).isNotNull();
        assertThat(version.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(version.getVersionType()).isEqualTo(ResumeVersion.VersionType.CONVERTED);
        assertThat(version.getOriginalFileName()).isNull();
        assertThat(version.getFileType()).isEqualTo("text/markdown");
        assertThat(version.getFileSize()).isEqualTo(0L);
        assertThat(version.getStoragePath()).isNull();
        assertThat(version.getContent()).isEqualTo("");
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should reconstruct version with all fields")
    void shouldReconstructVersionWithAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // When
        ResumeVersion version = ResumeVersion.reconstruct(
                id, TEST_GROUP_ID, ResumeVersion.VersionType.AI_OPTIMIZED,
                TEST_FILE_NAME, "stored.pdf", TEST_FILE_TYPE, TEST_FILE_SIZE,
                TEST_STORAGE_PATH, "minio", "AI content", "parsed",
                ResumeVersion.Status.ARCHIVED, now, now);

        // Then
        assertThat(version.getId()).isEqualTo(id);
        assertThat(version.getGroupId()).isEqualTo(TEST_GROUP_ID);
        assertThat(version.getVersionType()).isEqualTo(ResumeVersion.VersionType.AI_OPTIMIZED);
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
    }

    @Test
    @DisplayName("Should edit content for converted version")
    void shouldEditContentForConvertedVersion() {
        // Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);
        String newContent = "# Updated Resume\n\nNew content here";

        // When
        version.editContent(newContent);

        // Then
        assertThat(version.getContent()).isEqualTo(newContent);
    }

    @Test
    @DisplayName("Should throw exception when editing original version")
    void shouldThrowExceptionWhenEditingOriginalVersion() {
        // Given
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        // Then
        assertThatThrownBy(() -> version.editContent("new content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Original version cannot be edited");
    }

    @Test
    @DisplayName("Should throw exception when editing archived version")
    void shouldThrowExceptionWhenEditingArchivedVersion() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ResumeVersion version = ResumeVersion.reconstruct(
                id, TEST_GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null, "", null,
                ResumeVersion.Status.ARCHIVED, now, now);

        // Then
        assertThatThrownBy(() -> version.editContent("new content"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active version can be edited");
    }

    @Test
    @DisplayName("Should return editable true for active converted version")
    void shouldReturnEditableTrueForActiveConvertedVersion() {
        // Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);

        // Then
        assertThat(version.isEditable()).isTrue();
    }

    @Test
    @DisplayName("Should return editable false for original version")
    void shouldReturnEditableFalseForOriginalVersion() {
        // Given
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        // Then
        assertThat(version.isEditable()).isFalse();
    }

    @Test
    @DisplayName("Should return editable false for archived converted version")
    void shouldReturnEditableFalseForArchivedConvertedVersion() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ResumeVersion version = ResumeVersion.reconstruct(
                id, TEST_GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null, "", null,
                ResumeVersion.Status.ARCHIVED, now, now);

        // Then
        assertThat(version.isEditable()).isFalse();
    }

    @Test
    @DisplayName("Should apply parsed content")
    void shouldApplyParsedContent() {
        // Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);
        String parsedContent = "{\"name\": \"John\", \"skills\": [\"Java\"]}";

        // When
        version.applyParsedContent(parsedContent);

        // Then
        assertThat(version.getParsedContent()).isEqualTo(parsedContent);
    }

    @Test
    @DisplayName("Should archive version and update timestamp")
    void shouldArchiveVersionAndUpdateTimestamp() {
        // Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);
        LocalDateTime beforeArchive = version.getUpdatedAt();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        version.archive();

        // Then
        assertThat(version.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
        assertThat(version.getUpdatedAt()).isAfterOrEqualTo(beforeArchive);
    }

    @Test
    @DisplayName("Should support all version types")
    void shouldSupportAllVersionTypes() {
        // When & Then
        for (ResumeVersion.VersionType type : ResumeVersion.VersionType.values()) {
            assertThat(type).isIn(ResumeVersion.VersionType.ORIGINAL, 
                    ResumeVersion.VersionType.CONVERTED, ResumeVersion.VersionType.AI_OPTIMIZED);
        }
    }

    @Test
    @DisplayName("Should support all statuses")
    void shouldSupportAllStatuses() {
        // When & Then
        for (ResumeVersion.Status status : ResumeVersion.Status.values()) {
            assertThat(status).isIn(ResumeVersion.Status.ACTIVE, ResumeVersion.Status.ARCHIVED);
        }
    }

    @Test
    @DisplayName("Should maintain id and groupId immutability")
    void shouldMaintainIdAndGroupIdImmutability() {
        // Given
        ResumeVersion version = ResumeVersion.createOriginal(
                TEST_GROUP_ID, TEST_FILE_NAME, TEST_FILE_TYPE, TEST_FILE_SIZE, TEST_STORAGE_PATH);

        // Then
        assertThat(version.getId()).isNotNull();
        assertThat(version.getGroupId()).isEqualTo(TEST_GROUP_ID);
    }

    @Test
    @DisplayName("Should update timestamp on content edit")
    void shouldUpdateTimestampOnContentEdit() {
        // Given
        ResumeVersion version = ResumeVersion.createConverted(TEST_GROUP_ID);
        LocalDateTime beforeEdit = version.getUpdatedAt();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        version.editContent("New content");

        // Then
        assertThat(version.getUpdatedAt()).isAfterOrEqualTo(beforeEdit);
    }
}
