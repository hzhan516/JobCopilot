package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.command.CreateVersionCommand;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ResumeVersionChainManager 单元测试
 * 简历版本链管理器单元测试
 * <p>
 * 测试版本创建与链管理完整路径：
 * Tests the full version creation and chain management path:
 * - 从活跃转换版本创建
 * - Create from active converted version
 * - 从指定源版本创建
 * - Create from specified source version
 * - 链长度限制
 * - Chain length limit
 * - 传播解析内容
 * - Propagate parsed content
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Version Chain Manager Tests")
class ResumeVersionChainManagerTest {

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private VectorGenerationService vectorGenerationService;

    @InjectMocks
    private ResumeVersionChainManager manager;

    private UUID groupId;
    private UUID userId;
    private ResumeGroup group;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
        group = ResumeGroup.create(userId, "My Resume");
    }

    // ==================== 正常路径 ====================

    @Test
    @DisplayName("Should create version from active converted content")
    void shouldCreateVersionFromActiveConvertedContent() {
        // 给定 / Given
        ResumeVersion converted = ResumeVersion.createConverted(groupId);
        converted.editContent("# Existing Content");
        group.addVersion(converted);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        ResumeVersion result = manager.createVersion(command);

        // 那么 / Then
        assertThat(result.getContent()).isEqualTo("# Existing Content");
        assertThat(result.getVersionType()).isEqualTo(ResumeVersion.VersionType.CONVERTED);
        verify(vectorGenerationService).generateForResume(eq(result.getId()), eq("# Existing Content"));
    }

    @Test
    @DisplayName("Should create version from specified source version")
    void shouldCreateVersionFromSpecifiedSourceVersion() {
        // 给定 / Given
        UUID sourceVersionId = UUID.randomUUID();
        ResumeVersion source = ResumeVersion.createConverted(groupId);
        source.editContent("# Source Content");

        ResumeVersion converted = ResumeVersion.createConverted(groupId);
        converted.editContent("# Other Content");
        group.addVersion(converted);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, sourceVersionId);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.of(source));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        ResumeVersion result = manager.createVersion(command);

        // 那么 / Then
        assertThat(result.getContent()).isEqualTo("# Source Content");
        verify(vectorGenerationService).generateForResume(eq(result.getId()), eq("# Source Content"));
    }

    @Test
    @DisplayName("Should propagate parsed content to new version")
    void shouldPropagateParsedContentToNewVersion() {
        // 给定 / Given
        ResumeVersion original = ResumeVersion.createOriginal(
                groupId, "resume.pdf", "application/pdf", 1024L, "storage/path");
        original.markParseCompleted("{\"name\":\"John\"}");
        group.addVersion(original);

        ResumeVersion converted = ResumeVersion.createConverted(groupId);
        converted.editContent("# Content");
        group.addVersion(converted);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        ResumeVersion result = manager.createVersion(command);

        // 那么 / Then
        assertThat(result.getParsedContent()).isEqualTo("{\"name\":\"John\"}");
        assertThat(result.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should handle no active converted version")
    void shouldHandleNoActiveConvertedVersion() {
        // 给定 / Given
        ResumeVersion original = ResumeVersion.createOriginal(
                groupId, "resume.pdf", "application/pdf", 1024L, "storage/path");
        group.addVersion(original);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        ResumeVersion result = manager.createVersion(command);

        // 那么 / Then
        assertThat(result.getContent()).isEmpty();
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("Should throw when group not found")
    void shouldThrowWhenGroupNotFound() {
        // 给定 / Given
        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> manager.createVersion(command))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("group.not.found");
    }

    @Test
    @DisplayName("Should throw when source version not found")
    void shouldThrowWhenSourceVersionNotFound() {
        // 给定 / Given
        UUID sourceVersionId = UUID.randomUUID();
        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, sourceVersionId);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> manager.createVersion(command))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should throw when source version belongs to different group")
    void shouldThrowWhenSourceVersionBelongsToDifferentGroup() {
        // 给定 / Given
        UUID sourceVersionId = UUID.randomUUID();
        UUID otherGroupId = UUID.randomUUID();
        ResumeVersion source = ResumeVersion.createConverted(otherGroupId);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, sourceVersionId);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(versionRepository.findById(sourceVersionId)).thenReturn(Optional.of(source));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> manager.createVersion(command))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.group.mismatch");
    }

    // ==================== 链限制 ====================

    @Test
    @DisplayName("Should enforce max chain length by deleting oldest archived")
    void shouldEnforceMaxChainLengthByDeletingOldestArchived() {
        // 给定 / Given
        // Create 50 converted versions (at limit)
        for (int i = 0; i < 50; i++) {
            ResumeVersion v = ResumeVersion.createConverted(groupId);
            v.editContent("v" + i);
            group.addVersion(v);
            // Archive all but the last one
            if (i < 49) {
                v.archive();
            }
        }

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(versionRepository.findAllByGroupIdAndType(groupId, ResumeVersion.VersionType.CONVERTED))
                .thenReturn(group.getVersions().stream()
                        .filter(v -> v.getVersionType() == ResumeVersion.VersionType.CONVERTED)
                        .toList());
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        manager.createVersion(command);

        // 那么 / Then — verify oldest archived was deleted
        verify(versionRepository).findAllByGroupIdAndType(groupId, ResumeVersion.VersionType.CONVERTED);
    }

    @Test
    @DisplayName("Should not delete when under chain limit")
    void shouldNotDeleteWhenUnderChainLimit() {
        // 给定 / Given
        ResumeVersion converted = ResumeVersion.createConverted(groupId);
        converted.editContent("# Content");
        group.addVersion(converted);

        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(versionRepository.findAllByGroupIdAndType(groupId, ResumeVersion.VersionType.CONVERTED))
                .thenReturn(List.of(converted));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        manager.createVersion(command);

        // 那么 / Then
        verify(versionRepository, never()).delete(any(UUID.class));
    }

    @Test
    @DisplayName("Should skip vector generation when content is empty")
    void shouldSkipVectorGenerationWhenContentIsEmpty() {
        // 给定 / Given
        CreateVersionCommand command = new CreateVersionCommand(groupId, userId, null);
        when(groupRepository.findByIdAndUserId(groupId, userId)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(ResumeGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        // 当 / When
        ResumeVersion result = manager.createVersion(command);

        // 那么 / Then
        assertThat(result.getContent()).isEmpty();
        verify(vectorGenerationService, never()).generateForResume(any(), anyString());
    }
}
