package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ResumeAccessControl 单元测试
 * 简历访问控制单元测试
 * <p>
 * 测试所有权验证规则：
 * Tests ownership verification rules:
 * - 组级权限
 * - Group-level permission
 * - 版本级权限（级联验证组）
 * - Version-level permission (cascading group validation)
 * - 越权访问拒绝
 * - Unauthorized access denial
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Access Control Tests")
class ResumeAccessControlTest {

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @InjectMocks
    private ResumeAccessControl accessControl;

    private UUID userId;
    private UUID otherUserId;
    private UUID groupId;
    private UUID versionId;
    private ResumeGroup group;
    private ResumeVersion version;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        group = ResumeGroup.reconstruct(
                groupId, userId, "My Resume",
                false, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                java.util.Collections.emptyList()
        );

        version = ResumeVersion.reconstruct(
                versionId, groupId, ResumeVersion.VersionType.ORIGINAL,
                "resume.pdf", "storage/path", "application/pdf", 1024L,
                null, null, null, null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
    }

    // ==================== requireGroup ====================

    @Test
    @DisplayName("Should return group when owned by user")
    void shouldReturnGroupWhenOwnedByUser() {
        // 给定 / Given
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));

        // 当 / When
        ResumeGroup result = accessControl.requireGroup(groupId, userId);

        // 那么 / Then
        assertThat(result.getId()).isEqualTo(groupId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw when group not found for user")
    void shouldThrowWhenGroupNotFoundForUser() {
        // 给定 / Given
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireGroup(groupId, userId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("group.not.found");
    }

    @Test
    @DisplayName("Should throw when group belongs to another user")
    void shouldThrowWhenGroupBelongsToAnotherUser() {
        // 给定 / Given
        when(groupRepository.findByIdAndUserId(groupId, otherUserId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireGroup(groupId, otherUserId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("group.not.found");
    }

    // ==================== requireVersion ====================

    @Test
    @DisplayName("Should return version when owned by user")
    void shouldReturnVersionWhenOwnedByUser() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 当 / When
        ResumeVersion result = accessControl.requireVersion(versionId, userId);

        // 那么 / Then
        assertThat(result.getId()).isEqualTo(versionId);
    }

    @Test
    @DisplayName("Should throw when version not found")
    void shouldThrowWhenVersionNotFound() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireVersion(versionId, userId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should throw when version exists but group not found")
    void shouldThrowWhenVersionExistsButGroupNotFound() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireVersion(versionId, userId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("group.not.found");
    }

    @Test
    @DisplayName("Should throw access denied when version owned by another user")
    void shouldThrowAccessDeniedWhenVersionOwnedByAnotherUser() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireVersion(versionId, otherUserId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("access.denied");
    }

    // ==================== requireGroupForVersion ====================

    @Test
    @DisplayName("Should return group for version when owned")
    void shouldReturnGroupForVersionWhenOwned() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 当 / When
        ResumeGroup result = accessControl.requireGroupForVersion(versionId, userId);

        // 那么 / Then
        assertThat(result.getId()).isEqualTo(groupId);
    }

    @Test
    @DisplayName("Should throw when version not found for group lookup")
    void shouldThrowWhenVersionNotFoundForGroupLookup() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireGroupForVersion(versionId, userId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should throw access denied for group lookup when wrong user")
    void shouldThrowAccessDeniedForGroupLookupWhenWrongUser() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireGroupForVersion(versionId, otherUserId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("access.denied");
    }
}
