package edu.asu.ser594.resumeassistant.domain.resume.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 简历组实体单元测试
 * ResumeGroup Entity Unit Tests
 * <p>
 * 遵循DDD原则测试简历组聚合根：
 * Tests the ResumeGroup aggregate root following DDD principles:
 * - 工厂方法创建
 * - Factory method creation
 * - 聚合不变式（用户所有权、版本管理）
 * - Aggregate invariants (user ownership, version management)
 * - 业务规则（每种类型一个活跃版本）
 * - Business rules (one active version per type)
 */
@DisplayName("ResumeGroup Entity Tests")
class ResumeGroupTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_TITLE = "My Resume";

    @Test
    @DisplayName("Should create group with factory method and default title")
    void shouldCreateGroupWithFactoryMethodAndDefaultTitle() {
        // 当
        // When
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, null);

        // 那么
        // Then
        assertThat(group).isNotNull();
        assertThat(group.getId()).isNotNull();
        assertThat(group.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(group.getTitle()).isEqualTo("Untitled Resume");
        assertThat(group.isDefault()).isFalse();
        assertThat(group.getVersions()).isEmpty();
        assertThat(group.getCreatedAt()).isNotNull();
        assertThat(group.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create group with custom title")
    void shouldCreateGroupWithCustomTitle() {
        // 当
        // When
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);

        // 那么
        // Then
        assertThat(group.getTitle()).isEqualTo(TEST_TITLE);
    }

    @Test
    @DisplayName("Should upload original version and create converted version")
    void shouldUploadOriginalVersionAndCreateConvertedVersion() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);

        // 当
        // When
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");

        // 那么
        // Then
        assertThat(group.getVersions()).hasSize(2);
        assertThat(group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL)).isNotNull();
        assertThat(group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED)).isNotNull();
    }

    @Test
    @DisplayName("Should add version and auto-archive same type active version")
    void shouldAddVersionAndAutoArchiveSameTypeActiveVersion() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        ResumeVersion original = ResumeVersion.createOriginal(
                group.getId(), "v1.pdf", "application/pdf", 1024L, "path/v1");
        group.addVersion(original);

        // 当
        // When
        ResumeVersion newOriginal = ResumeVersion.createOriginal(
                group.getId(), "v2.pdf", "application/pdf", 2048L, "path/v2");
        group.addVersion(newOriginal);

        // 那么
        // Then
        assertThat(group.getVersions()).hasSize(2);
        assertThat(original.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
        assertThat(newOriginal.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should return unmodifiable versions list")
    void shouldReturnUnmodifiableVersionsList() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");

        List<ResumeVersion> versions = group.getVersions();

        // 那么
        // Then
        assertThatThrownBy(() -> versions.add(ResumeVersion.createConverted(group.getId())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should get active version by type")
    void shouldGetActiveVersionByType() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");

        // 当
        // When
        ResumeVersion original = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);
        ResumeVersion converted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        ResumeVersion ai = group.getActiveVersionByType(ResumeVersion.VersionType.AI_OPTIMIZED);

        // 那么
        // Then
        assertThat(original).isNotNull();
        assertThat(original.getVersionType()).isEqualTo(ResumeVersion.VersionType.ORIGINAL);
        assertThat(converted).isNotNull();
        assertThat(converted.getVersionType()).isEqualTo(ResumeVersion.VersionType.CONVERTED);
        assertThat(ai).isNull();
    }

    @Test
    @DisplayName("Should set as default")
    void shouldSetAsDefault() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        assertThat(group.isDefault()).isFalse();

        // 当
        // When
        group.setAsDefault();

        // 那么
        // Then
        assertThat(group.isDefault()).isTrue();
    }

    @Test
    @DisplayName("Should change title")
    void shouldChangeTitle() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, "Old Title");

        // 当
        // When
        group.changeTitle("New Title");

        // 那么
        // Then
        assertThat(group.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("Should not change title when null")
    void shouldNotChangeTitleWhenNull() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, "Original Title");

        // 当
        // When
        group.changeTitle(null);

        // 那么
        // Then
        assertThat(group.getTitle()).isEqualTo("Original Title");
    }

    @Test
    @DisplayName("Should verify ownership correctly")
    void shouldVerifyOwnershipCorrectly() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        UUID otherUserId = UUID.randomUUID();

        // 那么
        // Then
        assertThat(group.isOwnedBy(TEST_USER_ID)).isTrue();
        assertThat(group.isOwnedBy(otherUserId)).isFalse();
    }

    @Test
    @DisplayName("Should reconstruct aggregate with versions")
    void shouldReconstructAggregateWithVersions() {
        // 给定
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ResumeVersion version = ResumeVersion.createOriginal(
                id, "resume.pdf", "application/pdf", 1024L, "storage/path");

        // 当
        // When
        ResumeGroup group = ResumeGroup.reconstruct(
                id, TEST_USER_ID, TEST_TITLE, true, now, now, List.of(version));

        // 那么
        // Then
        assertThat(group.getId()).isEqualTo(id);
        assertThat(group.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(group.getTitle()).isEqualTo(TEST_TITLE);
        assertThat(group.isDefault()).isTrue();
        assertThat(group.getVersions()).hasSize(1);
    }

    @Test
    @DisplayName("Should update timestamp on modifications")
    void shouldUpdateTimestampOnModifications() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        LocalDateTime initialTimestamp = group.getUpdatedAt();

        // 当
        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
        group.changeTitle("New Title");
        LocalDateTime afterTitleChange = group.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
        group.setAsDefault();
        LocalDateTime afterSetDefault = group.getUpdatedAt();

        // 那么
        // Then
        assertThat(afterTitleChange).isAfter(initialTimestamp);
        assertThat(afterSetDefault).isAfter(afterTitleChange);
    }

    @Test
    @DisplayName("Should maintain userId immutability")
    void shouldMaintainUserIdImmutability() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);

        // 那么
        // Then
        assertThat(group.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should maintain createdAt immutability")
    void shouldMaintainCreatedAtImmutability() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        LocalDateTime createdAt = group.getCreatedAt();

        // 当
        // When
        group.changeTitle("New");
        group.setAsDefault();
        group.uploadOriginalVersion("file.pdf", "application/pdf", 1024L, "path");

        // 那么
        // Then
        assertThat(group.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should activate archived version and auto-archive current active")
    void shouldActivateArchivedVersionAndAutoArchiveCurrentActive() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");

        ResumeVersion firstConverted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        ResumeVersion secondConverted = ResumeVersion.createConverted(group.getId());
        group.addVersion(secondConverted);

        // firstConverted 应已被归档
        // firstConverted should be archived
        assertThat(firstConverted.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
        assertThat(secondConverted.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);

        // 当 - 激活第一个版本
        // When - activate the first version
        group.activateVersion(firstConverted.getId());

        // 那么
        // Then
        assertThat(firstConverted.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
        assertThat(secondConverted.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
        assertThat(group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED))
                .isEqualTo(firstConverted);
    }

    @Test
    @DisplayName("Should throw when activating non-existent version")
    void shouldThrowWhenActivatingNonExistentVersion() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");
        UUID nonExistent = UUID.randomUUID();

        // 那么
        // Then
        assertThatThrownBy(() -> group.activateVersion(nonExistent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Version not found in group");
    }

    @Test
    @DisplayName("Should throw when activating already active version")
    void shouldThrowWhenActivatingAlreadyActiveVersion() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");
        ResumeVersion activeConverted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);

        // 那么
        // Then
        assertThatThrownBy(() -> group.activateVersion(activeConverted.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    @DisplayName("Should throw when activating original version")
    void shouldThrowWhenActivatingOriginalVersion() {
        // 给定
        // Given
        ResumeGroup group = ResumeGroup.create(TEST_USER_ID, TEST_TITLE);
        group.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "storage/path");
        ResumeVersion original = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);

        // 那么
        // Then
        assertThatThrownBy(() -> group.activateVersion(original.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Original version cannot be activated");
    }
}
