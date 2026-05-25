package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.command.ResumeUploadCommand;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.ResumeConverterService;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeUploadHandler 单元测试
 * 简历上传处理器单元测试
 * <p>
 * 测试简历上传完整工作流：
 * Tests the complete resume upload workflow:
 * - 文件存储与版本创建
 * - File storage and version creation
 * - 自动转换与向量生成
 * - Auto-conversion and vector generation
 * - 异步解析命令发布
 * - Async parse command publication
 * - 发布失败降级
 * - Publish failure fallback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Upload Handler Tests")
class ResumeUploadHandlerTest {

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ResumeConverterService resumeConverterService;

    @Mock
    private VectorGenerationService vectorGenerationService;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @InjectMocks
    private ResumeUploadHandler handler;

    private UUID userId;
    private InputStream inputStream;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        inputStream = new ByteArrayInputStream("PDF content".getBytes());
    }

    // ==================== 正常路径 ====================
    // ==================== Happy Path ====================

    @Test
    @DisplayName("Should create group and versions on upload")
    void shouldCreateGroupAndVersionsOnUpload() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), eq("application/pdf")))
                .thenReturn("# Converted Markdown");
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned-url");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        ResumeGroup result = handler.upload(command, userId);

        // 那么 / Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo("My Resume");

        verify(fileStorageService).upload(anyString(), eq(inputStream), eq(1024L), eq("application/pdf"));
        verify(groupRepository).save(any(ResumeGroup.class));
    }

    @Test
    @DisplayName("Should auto-convert and generate vector when markdown available")
    void shouldAutoConvertAndGenerateVectorWhenMarkdownAvailable() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .fileSize(2048L)
                .inputStream(inputStream)
                .title("Word Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn("# Converted from Word");
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        ResumeGroup result = handler.upload(command, userId);

        // 那么 / Then
        ResumeVersion converted = result.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        assertThat(converted).isNotNull();

        verify(resumeConverterService).convertToMarkdown(anyString(), eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        verify(vectorGenerationService).generateForResume(any(UUID.class), eq("# Converted from Word"));
    }

    @Test
    @DisplayName("Should skip vector generation when conversion returns null")
    void shouldSkipVectorGenerationWhenConversionReturnsNull() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        handler.upload(command, userId);

        // 那么 / Then
        verify(vectorGenerationService, never()).generateForResume(any(), anyString());
    }

    @Test
    @DisplayName("Should publish resume parse command with presigned URL")
    void shouldPublishResumeParseCommandWithPresignedUrl() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned-url");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        handler.upload(command, userId);

        // 那么 / Then
        ArgumentCaptor<ResumeParseCommand> captor = ArgumentCaptor.forClass(ResumeParseCommand.class);
        verify(aiMessagePublisherPort).sendResumeForParsing(captor.capture());

        ResumeParseCommand published = captor.getValue();
        assertThat(published.resumeId()).isNotNull();
        assertThat(published.fileUrl()).isEqualTo("https://minio.example.com/presigned-url");
        assertThat(published.format()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should mark original version as parsing")
    void shouldMarkOriginalVersionAsParsing() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        ResumeGroup result = handler.upload(command, userId);

        // 那么 / Then
        ResumeVersion original = result.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst()
                .orElseThrow();
        assertThat(original.getParseStatus().name()).isEqualTo("PARSING");
    }

    // ==================== 异常路径 ====================
    // ==================== Exception Path ====================

    @Test
    @DisplayName("Should handle publish failure gracefully and mark parse failed")
    void shouldHandlePublishFailureGracefullyAndMarkParseFailed() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("My Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        doThrow(new RuntimeException("MQ unavailable"))
                .when(aiMessagePublisherPort).sendResumeForParsing(any(ResumeParseCommand.class));

        // 当 / When — should not throw
        ResumeGroup result = handler.upload(command, userId);

        // 那么 / Then
        assertThat(result).isNotNull();

        // Verify that the original version was marked as FAILED
        // 验证原始版本被标记为 FAILED
        ResumeVersion original = result.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst()
                .orElseThrow();
        assertThat(original.getParseStatus().name()).isEqualTo("FAILED");
        assertThat(original.getParseErrorMessage()).contains("Failed to publish parsing request");
    }

    // ==================== 边界条件 ====================
    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should sanitize filename with special characters")
    void shouldSanitizeFilenameWithSpecialCharacters() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName("my:resume*with?special\u003cchars\u003e|here.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("Special Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        handler.upload(command, userId);

        // 那么 / Then — verify that upload path contains sanitized name
        // 验证上传路径包含已清理的文件名
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(pathCaptor.capture(), any(), anyLong(), anyString());
        assertThat(pathCaptor.getValue()).doesNotContain(":").doesNotContain("*").doesNotContain("?");
    }

    @Test
    @DisplayName("Should handle null filename")
    void shouldHandleNullFilename() {
        // 给定 / Given
        ResumeUploadCommand command = ResumeUploadCommand.builder()
                .fileName(null)
                .contentType("application/pdf")
                .fileSize(1024L)
                .inputStream(inputStream)
                .title("Null Filename Resume")
                .build();

        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        when(resumeConverterService.convertToMarkdown(anyString(), anyString()))
                .thenReturn(null);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class)))
                .thenReturn("https://minio.example.com/presigned");
        doNothing().when(versionRepository).save(any(ResumeVersion.class));

        // 当 / When
        handler.upload(command, userId);

        // 那么 / Then — should use "unnamed" as fallback
        // 应使用 "unnamed" 作为回退
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(pathCaptor.capture(), any(), anyLong(), anyString());
        assertThat(pathCaptor.getValue()).endsWith("_unnamed");
    }
}
