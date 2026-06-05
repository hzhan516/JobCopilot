package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ResumeParseResultHandler 单元测试
 * 简历解析结果处理器单元测试
 * <p>
 * 测试异步 AI 解析结果处理的完整路径：
 * Tests the complete async AI parse result handling path:
 * - 成功结果处理与传播
 * - Success result processing and propagation
 * - 失败结果降级
 * - Failure result fallback
 * - 版本不存在异常
 * - Version not found exception
 * - 异常处理内部降级
 * - Internal exception handling fallback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Parse Result Handler Tests")
class ResumeParseResultHandlerTest {

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private VectorGenerationService vectorGenerationService;

    @InjectMocks
    private ResumeParseResultHandler handler;

    private UUID versionId;
    private ResumeVersion originalVersion;
    private ResumeGroup group;

    @BeforeEach
    void setUp() {
        versionId = UUID.randomUUID();

        group = ResumeGroup.create(UUID.randomUUID(), "Test Resume");
        // Use reflection or reconstruct to get a version with known ID
        // 使用重建来获得已知 ID 的版本
        originalVersion = ResumeVersion.createOriginal(
                group.getId(), "resume.pdf", "application/pdf", 1024L, "storage/path");

        // We need to reconstruct with specific versionId for findById matching
        // 需要使用特定 versionId 重建以匹配 findById
        originalVersion = ResumeVersion.reconstruct(
                versionId, group.getId(), ResumeVersion.VersionType.ORIGINAL,
                "resume.pdf", "storage/path", "application/pdf", 1024L,
                null, null, null, null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
    }

    // ==================== 成功路径 ====================
    // ==================== Happy Path ====================

    @Test
    @DisplayName("Should mark original as completed and propagate to derived versions")
    void shouldMarkOriginalAsCompletedAndPropagateToDerivedVersions() {
        // 给定 / Given
        ResumeVersion converted = ResumeVersion.createConverted(group.getId());

        // Reconstruct group with versions
        // 使用版本重建组
        group = ResumeGroup.reconstruct(
                group.getId(), group.getUserId(), group.getTitle(),
                false, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                new java.util.ArrayList<>(List.of(originalVersion, converted)), 0L
        );

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        Map<String, Object> data = Map.of("parsedContent", Map.of(
                "name", "John Doe",
                "skills", List.of("Java", "Spring"),
                "experience", List.of(Map.of("title", "Dev"))
        ));
        AiResultEvent event = new AiResultEvent(versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then
        assertThat(originalVersion.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
        assertThat(originalVersion.getParsedContent()).isNotNull();
        verify(vectorGenerationService).generateForResume(eq(versionId), anyString());
    }

    @Test
    @DisplayName("Should propagate parsed content to non-original non-completed versions")
    void shouldPropagateParsedContentToNonOriginalNonCompletedVersions() {
        // 给定 / Given
        ResumeVersion converted = ResumeVersion.createConverted(group.getId());
        // Reconstruct with pending status
        // 使用 PENDING 状态重建
        ResumeVersion convertedReconstruct = ResumeVersion.reconstruct(
                UUID.randomUUID(), group.getId(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                null, null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        group = ResumeGroup.reconstruct(
                group.getId(), group.getUserId(), group.getTitle(),
                false, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                new java.util.ArrayList<>(List.of(originalVersion, convertedReconstruct)), 0L
        );

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        Map<String, Object> data = Map.of("parsedContent", Map.of("name", "John"));
        AiResultEvent event = new AiResultEvent(versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then — verify converted version was also updated
        // 验证转换版本也被更新
        assertThat(convertedReconstruct.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
        verify(versionRepository, atLeast(2)).save(any(ResumeVersion.class));
    }

    @Test
    @DisplayName("Should skip propagation when derived version already completed")
    void shouldSkipPropagationWhenDerivedVersionAlreadyCompleted() {
        // 给定 / Given
        ResumeVersion converted = ResumeVersion.reconstruct(
                UUID.randomUUID(), group.getId(), ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                null, null, ParseStatus.COMPLETED, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        group = ResumeGroup.reconstruct(
                group.getId(), group.getUserId(), group.getTitle(),
                false, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                new java.util.ArrayList<>(List.of(originalVersion, converted)), 0L
        );

        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        Map<String, Object> data = Map.of("parsedContent", Map.of("name", "John"));
        AiResultEvent event = new AiResultEvent(versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then — original saved, but converted not saved again
        // 原始版本已保存，但转换版本未被再次保存
        verify(versionRepository, times(2)).save(any(ResumeVersion.class));
    }

    @Test
    @DisplayName("Should skip propagation when group not found")
    void shouldSkipPropagationWhenGroupNotFound() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.empty());

        Map<String, Object> data = Map.of("parsedContent", Map.of("name", "John"));
        AiResultEvent event = new AiResultEvent(versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then — original is completed, no propagation attempted
        // 原始版本已完成，未尝试传播
        assertThat(originalVersion.getParseStatus()).isEqualTo(ParseStatus.COMPLETED);
        verify(groupRepository).findById(group.getId());
    }

    // ==================== 失败路径 ====================
    // ==================== Failure Path ====================

    @Test
    @DisplayName("Should mark parse failed when AI status is FAILED")
    void shouldMarkParseFailedWhenAiStatusIsFailed() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        AiResultEvent event = new AiResultEvent(
                versionId.toString(), "RESUME_PARSE", "FAILED",
                null, "AI parsing service timeout", null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then
        assertThat(originalVersion.getParseStatus()).isEqualTo(ParseStatus.FAILED);
        assertThat(originalVersion.getParseErrorMessage()).isEqualTo("AI parsing service timeout");
    }

    @Test
    @DisplayName("Should use default error message when AI error is null")
    void shouldUseDefaultErrorMessageWhenAiErrorIsNull() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        AiResultEvent event = new AiResultEvent(
                versionId.toString(), "RESUME_PARSE", "FAILED", null, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then
        assertThat(originalVersion.getParseStatus()).isEqualTo(ParseStatus.FAILED);
        assertThat(originalVersion.getParseErrorMessage()).isEqualTo("Unknown error");
    }

    // ==================== 异常路径 ====================
    // ==================== Exception Path ====================

    @Test
    @DisplayName("Should throw StorageException when version not found")
    void shouldThrowStorageExceptionWhenVersionNotFound() {
        // 给定 / Given
        UUID unknownId = UUID.randomUUID();
        when(versionRepository.findById(unknownId)).thenReturn(Optional.empty());

        AiResultEvent event = new AiResultEvent(
                unknownId.toString(), "RESUME_PARSE", "COMPLETED",
                Map.of("parsedContent", Map.of()), null, null);

        // 当&那么 / When & Then
        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should handle exception during data processing gracefully")
    void shouldHandleExceptionDuringDataProcessingGracefully() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // Invalid data that will cause serialization to fail
        // 会导致序列化失败的无效数据
        Map<String, Object> data = Map.of("parsedContent", new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("Serialization failed");
            }
        });
        AiResultEvent event = new AiResultEvent(
                versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When — should not throw, should mark failed
        handler.handle(event);

        // 那么 / Then
        assertThat(originalVersion.getParseStatus()).isEqualTo(ParseStatus.FAILED);
        assertThat(originalVersion.getParseErrorMessage()).contains("Failed to handle parsed result");
    }

    @Test
    @DisplayName("Should generate vector for parsed content")
    void shouldGenerateVectorForParsedContent() {
        // 给定 / Given
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(originalVersion));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.empty());

        Map<String, Object> parsedContent = Map.of(
                "name", "John",
                "skills", List.of("Java"),
                "summary", "Experienced developer"
        );
        Map<String, Object> data = Map.of("parsedContent", parsedContent);
        AiResultEvent event = new AiResultEvent(versionId.toString(), "RESUME_PARSE", "COMPLETED", data, null, null);

        // 当 / When
        handler.handle(event);

        // 那么 / Then
        verify(vectorGenerationService).generateForResume(eq(versionId), anyString());
    }
}
