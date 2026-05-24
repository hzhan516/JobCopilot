package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ResumeDeletionService 单元测试
 * 简历删除服务单元测试
 * <p>
 * 测试物理与逻辑删除完整路径：
 * Tests the full physical and logical deletion path:
 * - 删除组及所有版本
 * - Delete group and all versions
 * - 删除单个版本
 * - Delete single version
 * - 文件删除失败降级
 * - File deletion failure fallback
 * - 空存储路径跳过
 * - Null storage path skip
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Deletion Service Tests")
class ResumeDeletionServiceTest {

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ResumeDeletionService service;

    private UUID groupId;
    private ResumeVersion originalVersion;
    private ResumeVersion convertedVersion;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        UUID originalId = UUID.randomUUID();
        UUID convertedId = UUID.randomUUID();
        originalVersion = ResumeVersion.reconstruct(
                originalId, groupId, ResumeVersion.VersionType.ORIGINAL,
                "resume.pdf", "storage/resume.pdf", "application/pdf", 1024L,
                null, null, null, null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        convertedVersion = ResumeVersion.reconstruct(
                convertedId, groupId, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L,
                null, null, null, null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
    }

    // ==================== 删除组 ====================

    @Test
    @DisplayName("Should delete group and all versions")
    void shouldDeleteGroupAndAllVersions() {
        // 给定 / Given
        when(versionRepository.findAllByGroupId(groupId))
                .thenReturn(List.of(originalVersion, convertedVersion));

        // 当 / When
        service.deleteGroup(groupId);

        // 那么 / Then
        verify(fileStorageService).delete("storage/resume.pdf");
        verify(fileStorageService, never()).delete(eq(null));
        verify(versionRepository).deleteAllByGroupId(groupId);
        verify(groupRepository).delete(groupId);
    }

    @Test
    @DisplayName("Should handle file delete failure gracefully when deleting group")
    void shouldHandleFileDeleteFailureGracefullyWhenDeletingGroup() {
        // 给定 / Given
        when(versionRepository.findAllByGroupId(groupId))
                .thenReturn(List.of(originalVersion));
        doThrow(new RuntimeException("S3 error")).when(fileStorageService).delete("storage/resume.pdf");

        // 当 / When — should not throw
        service.deleteGroup(groupId);

        // 那么 / Then
        verify(versionRepository).deleteAllByGroupId(groupId);
        verify(groupRepository).delete(groupId);
    }

    // ==================== 删除版本 ====================

    @Test
    @DisplayName("Should delete version and its file")
    void shouldDeleteVersionAndItsFile() {
        // 当 / When
        service.deleteVersion(originalVersion);

        // 那么 / Then
        verify(fileStorageService).delete("storage/resume.pdf");
        verify(versionRepository).delete(originalVersion.getId());
    }

    @Test
    @DisplayName("Should delete converted version without storage")
    void shouldDeleteConvertedVersionWithoutStorage() {
        // 当 / When
        service.deleteVersion(convertedVersion);

        // 那么 / Then
        verify(fileStorageService, never()).delete(anyString());
        verify(versionRepository).delete(convertedVersion.getId());
    }

    @Test
    @DisplayName("Should handle file delete failure gracefully when deleting version")
    void shouldHandleFileDeleteFailureGracefullyWhenDeletingVersion() {
        // 给定 / Given
        doThrow(new RuntimeException("S3 error")).when(fileStorageService).delete("storage/resume.pdf");

        // 当 / When — should not throw
        service.deleteVersion(originalVersion);

        // 那么 / Then
        verify(versionRepository).delete(originalVersion.getId());
    }

    // ==================== 边界条件 ====================

    @Test
    @DisplayName("Should delete group with no versions")
    void shouldDeleteGroupWithNoVersions() {
        // 给定 / Given
        when(versionRepository.findAllByGroupId(groupId)).thenReturn(List.of());

        // 当 / When
        service.deleteGroup(groupId);

        // 那么 / Then
        verify(fileStorageService, never()).delete(anyString());
        verify(versionRepository).deleteAllByGroupId(groupId);
        verify(groupRepository).delete(groupId);
    }
}
