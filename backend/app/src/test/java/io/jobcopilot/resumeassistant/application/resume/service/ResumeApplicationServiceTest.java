package io.jobcopilot.resumeassistant.application.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.application.resume.command.CreateVersionCommand;
import io.jobcopilot.resumeassistant.application.resume.command.ResumeEditCommand;
import io.jobcopilot.resumeassistant.application.resume.command.ResumeUploadCommand;
import io.jobcopilot.resumeassistant.application.resume.dto.ResumeDownloadResult;
import io.jobcopilot.resumeassistant.application.resume.query.ResumeDownloadQuery;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 简历应用服务单元测试 / Resume application service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Application Service Tests")
class ResumeApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ResumeGroupRepository groupRepository;

    @Mock
    private ResumeVersionRepository versionRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private DocumentFormatConverter documentFormatConverter;

    @Mock
    private AiMessagePublisherPort aiMessagePublisherPort;

    @Mock
    private VectorFacade vectorFacade;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ResumeApplicationService resumeService;

    private ResumeGroup testGroup;
    private ResumeVersion testVersion;
    private InputStream testInputStream;

    // 准备测试数据 / Prepare test data
    @BeforeEach
    void setUp() {
        testInputStream = new ByteArrayInputStream("test content".getBytes());
    }

    @Test
    @DisplayName("Should handle resume upload successfully")
    void shouldHandleResumeUploadSuccessfully() throws Exception {
        // 准备 / Given
        ResumeUploadCommand command = new ResumeUploadCommand(
                "resume.pdf",
                "application/pdf",
                1024L,
                testInputStream,
                "My Resume"
        );

        InputStream downloadedStream = new ByteArrayInputStream("markdown content".getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.download(anyString())).thenReturn(Optional.of(downloadedStream));
        doReturn(new ByteArrayInputStream("markdown content".getBytes(StandardCharsets.UTF_8)))
                .when(documentFormatConverter).convert(any(), eq("pdf"), eq("md"));
        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 执行 / When
        ResumeGroup result = resumeService.handleUpload(command, USER_ID);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTitle()).isEqualTo("My Resume");
        verify(fileStorageService).upload(anyString(), eq(testInputStream), eq(1024L), eq("application/pdf"));
        verify(groupRepository).save(any(ResumeGroup.class));
        verify(aiMessagePublisherPort).sendResumeForParsing(any(ResumeParseCommand.class));
        // 验证 CONVERTED 版本触发了向量生成
        verify(vectorFacade).generateAndSaveVector(anyString(), eq("RESUME"), eq("markdown content"));
    }

    @Test
    @DisplayName("Should handle resume edit successfully")
    void shouldHandleResumeEditSuccessfully() {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        ResumeEditCommand command = new ResumeEditCommand(
                VERSION_ID,
                USER_ID,
                "Updated markdown content"
        );

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 执行 / When
        ResumeVersion result = resumeService.handleEdit(command);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated markdown content");
        verify(versionRepository).save(testVersion);
        verify(vectorFacade).generateAndSaveVector(eq(VERSION_ID.toString()), eq("RESUME"), eq("Updated markdown content"));
    }

    @Test
    @DisplayName("Should trigger vector gen when editing AI_OPTIMIZED version")
    void shouldTriggerVectorGenWhenEditingAiOptimizedVersion() {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.AI_OPTIMIZED);

        ResumeEditCommand command = new ResumeEditCommand(
                VERSION_ID,
                USER_ID,
                "Updated AI content"
        );

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 执行 / When
        ResumeVersion result = resumeService.handleEdit(command);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated AI content");
        verify(vectorFacade).generateAndSaveVector(eq(VERSION_ID.toString()), eq("RESUME"), eq("Updated AI content"));
    }

    @Test
    @DisplayName("Should succeed edit even when vector gen fails")
    void shouldSucceedEditEvenWhenVectorGenFails() {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);

        ResumeEditCommand command = new ResumeEditCommand(
                VERSION_ID,
                USER_ID,
                "Updated content"
        );

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(testVersion.getGroupId())).thenReturn(Optional.of(testGroup));
        doNothing().when(versionRepository).save(any(ResumeVersion.class));
        doThrow(new RuntimeException("AI service unavailable"))
                .when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        // 执行 / When
        ResumeVersion result = resumeService.handleEdit(command);

        // 验证 / Then — 编辑应成功返回，不因MQ异常而失败
        // Edit should succeed and return normally, not fail due to MQ exception
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Updated content");
        verify(versionRepository).save(testVersion);
        verify(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should process parse result successfully")
    void shouldProcessParseResultSuccessfully() {
        // 准备 / Given
        testVersion = createTestVersion(ResumeVersion.VersionType.ORIGINAL);
        when(versionRepository.findById(any())).thenReturn(Optional.of(testVersion));

        AiResultEvent event = new AiResultEvent(
                VERSION_ID.toString(),
                "RESUME_PARSE",
                "COMPLETED",
                Map.of("name", "John Doe"),
                null,
                "RESUME"
        );

        // 执行 / When
        resumeService.handleParseResult(event);

        // 验证 / Then
        verify(versionRepository, times(1)).save(testVersion);
        verify(vectorFacade).generateAndSaveVector(anyString(), eq("RESUME"), any());
    }

    // 创建测试简历组 / Create test resume group
    private ResumeGroup createTestGroup() {
        ResumeGroup group = ResumeGroup.create(USER_ID, "Test Resume");
        return ResumeGroup.reconstruct(
                GROUP_ID, USER_ID, "Test Resume", false,
                group.getCreatedAt(), group.getUpdatedAt(), Collections.emptyList()
        );
    }

    // ==================== 创建副本测试 ====================
    // ==================== Create Version Tests ====================

    @Test
    @DisplayName("Should create version copy from active converted")
    void shouldCreateVersionCopyFromActiveConverted() {
        // 准备 / Given
        testGroup = createTestGroup();
        // 向组中上传原版，自动创建 CONVERTED 版本
        // Upload original to group, which auto-creates CONVERTED version
        testGroup.uploadOriginalVersion("resume.pdf", "application/pdf", 1024L, "path/to/file");
        ResumeVersion activeConverted = testGroup.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        assertThat(activeConverted).isNotNull();
        activeConverted.editContent("Existing markdown content");

        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));
        when(versionRepository.findAllByGroupIdAndType(GROUP_ID, ResumeVersion.VersionType.CONVERTED))
                .thenReturn(List.of(activeConverted));
        doNothing().when(groupRepository).save(any(ResumeGroup.class));
        doNothing().when(vectorFacade).generateAndSaveVector(anyString(), anyString(), anyString());

        CreateVersionCommand command = CreateVersionCommand.builder()
                .groupId(GROUP_ID)
                .sourceVersionId(null)
                .userId(USER_ID)
                .build();

        // 执行 / When
        ResumeVersion result = resumeService.handleCreateVersion(command);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.getVersionType()).isEqualTo(ResumeVersion.VersionType.CONVERTED);
        assertThat(result.getContent()).isEqualTo("Existing markdown content");
        assertThat(result.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
        // 新版本通过 groupRepository.save 级联保存，不再直接调用 versionRepository.save
        // New version is cascade-saved via groupRepository.save, no direct versionRepository.save call
        verify(groupRepository).save(any(ResumeGroup.class));
        // 验证新 CONVERTED 版本触发了向量生成
        verify(vectorFacade).generateAndSaveVector(anyString(), eq("RESUME"), eq("Existing markdown content"));
    }

    @Test
    @DisplayName("Should create version copy from specific source version")
    void shouldCreateVersionCopyFromSpecificSourceVersion() {
        // 准备 / Given
        testGroup = createTestGroup();
        UUID sourceId = UUID.randomUUID();
        ResumeVersion sourceVersion = ResumeVersion.reconstruct(
                sourceId, GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "Source content", null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));
        when(versionRepository.findById(sourceId)).thenReturn(Optional.of(sourceVersion));
        when(versionRepository.findAllByGroupIdAndType(GROUP_ID, ResumeVersion.VersionType.CONVERTED))
                .thenReturn(List.of(sourceVersion));
        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        CreateVersionCommand command = CreateVersionCommand.builder()
                .groupId(GROUP_ID)
                .sourceVersionId(sourceId)
                .userId(USER_ID)
                .build();

        // 执行 / When
        ResumeVersion result = resumeService.handleCreateVersion(command);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Source content");
        verify(versionRepository).findById(sourceId);
    }

    @Test
    @DisplayName("Should delete oldest archived version when chain exceeds limit")
    void shouldDeleteOldestArchivedVersionWhenChainExceedsLimit() {
        // 准备 / Given
        testGroup = createTestGroup();
        // 构造 50 个 ARCHIVED 版本 + 1 个 ACTIVE 版本
        // Construct 50 ARCHIVED versions + 1 ACTIVE version
        java.util.List<ResumeVersion> chain = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ResumeVersion archived = ResumeVersion.reconstruct(
                    UUID.randomUUID(), GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                    null, null, "text/markdown", 0L, null, null,
                    "Archived " + i, null, ParseStatus.PENDING, null,
                    ResumeVersion.Status.ARCHIVED,
                    java.time.LocalDateTime.now().minusDays(100 - i),
                    java.time.LocalDateTime.now().minusDays(100 - i)
            );
            chain.add(archived);
        }
        ResumeVersion activeConverted = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        activeConverted.editContent("Active content");
        chain.add(activeConverted);

        when(groupRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testGroup));
        when(versionRepository.findAllByGroupIdAndType(GROUP_ID, ResumeVersion.VersionType.CONVERTED))
                .thenReturn(chain);
        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        CreateVersionCommand command = CreateVersionCommand.builder()
                .groupId(GROUP_ID)
                .sourceVersionId(null)
                .userId(USER_ID)
                .build();

        // 执行 / When
        ResumeVersion result = resumeService.handleCreateVersion(command);

        // 验证 / Then
        assertThat(result).isNotNull();
        // 应删除最旧的 ARCHIVED 版本（createdAt 最早的那个）
        // Should delete the oldest ARCHIVED version (earliest createdAt)
        verify(versionRepository).delete(chain.get(0).getId());
    }

    @Test
    @DisplayName("Should activate archived version and archive previous active")
    void shouldActivateArchivedVersionAndArchivePreviousActive() {
        // 准备 / Given
        UUID archivedId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();

        ResumeVersion activeVersion = ResumeVersion.reconstruct(
                activeId, GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "Active content", null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        ResumeVersion archivedVersion = ResumeVersion.reconstruct(
                archivedId, GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "Archived content", null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ARCHIVED, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );

        testGroup = createTestGroup();
        // 使用 reconstruct 直接构建包含两个版本的组，避免 addVersion 触发归档规则
        // Build group with both versions via reconstruct to avoid addVersion's archive rule
        testGroup = ResumeGroup.reconstruct(
                GROUP_ID, USER_ID, "Test Resume", false,
                testGroup.getCreatedAt(), testGroup.getUpdatedAt(),
                new java.util.ArrayList<>(List.of(activeVersion, archivedVersion))
        );

        when(versionRepository.findById(archivedId)).thenReturn(Optional.of(archivedVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        // 执行 / When
        ResumeVersion result = resumeService.handleActivateVersion(archivedId, USER_ID);

        // 验证 / Then
        assertThat(result.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
        assertThat(activeVersion.getStatus()).isEqualTo(ResumeVersion.Status.ARCHIVED);
        verify(groupRepository).save(any(ResumeGroup.class));
    }

    @Test
    @DisplayName("Should throw when activating version not owned by user")
    void shouldThrowWhenActivatingVersionNotOwnedByUser() {
        // 准备 / Given
        UUID otherUserId = UUID.randomUUID();
        testGroup = createTestGroup();
        ResumeVersion version = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));

        // 执行 & 验证 / When & Then
        assertThatThrownBy(() -> resumeService.handleActivateVersion(VERSION_ID, otherUserId))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("access.denied");
    }

    // ==================== 下载测试 ====================
    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should download original resume successfully")
    void shouldDownloadOriginalResumeSuccessfully() {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.ORIGINAL);
        InputStream fileStream = new ByteArrayInputStream("PDF content".getBytes(StandardCharsets.UTF_8));

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        when(fileStorageService.download("path/to/file")).thenReturn(Optional.of(fileStream));

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("original")
                .build();

        // 执行 / When
        ResumeDownloadResult result = resumeService.handleDownload(query);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.fileName()).isEqualTo("resume.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should download converted resume as PDF")
    void shouldDownloadConvertedResumeAsPdf() throws IOException {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        testVersion.editContent("# Resume\n\nContent");
        InputStream convertedStream = new ByteArrayInputStream("PDF bytes".getBytes(StandardCharsets.UTF_8));

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        when(documentFormatConverter.convert(any(), eq("md"), eq("pdf"))).thenReturn(convertedStream);

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("pdf")
                .build();

        // 执行 / When
        ResumeDownloadResult result = resumeService.handleDownload(query);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.fileName()).isEqualTo("resume.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        verify(documentFormatConverter).convert(any(), eq("md"), eq("pdf"));
    }

    @Test
    @DisplayName("Should download converted resume as HTML via built-in converter")
    void shouldDownloadConvertedResumeAsHtml() throws IOException {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        testVersion.editContent("# Resume\n\nContent");
        InputStream htmlStream = new ByteArrayInputStream("<h1>Resume</h1>".getBytes(StandardCharsets.UTF_8));

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        when(documentFormatConverter.convert(any(), eq("md"), eq("html"))).thenReturn(htmlStream);

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("html")
                .build();

        // 执行 / When
        ResumeDownloadResult result = resumeService.handleDownload(query);

        // 验证 / Then
        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("text/html");
    }

    @Test
    @DisplayName("Should throw when conversion fails")
    void shouldThrowWhenConversionFails() throws IOException {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        testVersion.editContent("# Resume");

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        when(documentFormatConverter.convert(any(), eq("md"), eq("docx")))
                .thenThrow(new IOException("Pandoc not available"));

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("docx")
                .build();

        // 执行与验证 / When & Then
        assertThatThrownBy(() -> resumeService.handleDownload(query))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("conversion.failed");
    }

    @Test
    @DisplayName("Should throw when version not found during download")
    void shouldThrowWhenVersionNotFoundDuringDownload() {
        // 准备 / Given
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(USER_ID)
                .targetFormat("pdf")
                .build();

        // 执行与验证 / When & Then
        assertThatThrownBy(() -> resumeService.handleDownload(query))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("version.not.found");
    }

    @Test
    @DisplayName("Should throw when access denied during download")
    void shouldThrowWhenAccessDeniedDuringDownload() {
        // 准备 / Given
        testGroup = createTestGroup();
        testVersion = createTestVersion(ResumeVersion.VersionType.CONVERTED);
        UUID otherUserId = UUID.randomUUID();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));

        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(VERSION_ID)
                .userId(otherUserId)
                .targetFormat("pdf")
                .build();

        // 执行与验证 / When & Then
        assertThatThrownBy(() -> resumeService.handleDownload(query))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("access.denied");
    }

    // 创建测试简历版本 / Create test resume version
    private ResumeVersion createTestVersion(ResumeVersion.VersionType type) {
        if (type == ResumeVersion.VersionType.ORIGINAL) {
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, "resume.pdf", "stored.pdf",
                    "application/pdf", 1024L, "path/to/file", "minio",
                    null, null, ParseStatus.PENDING, null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        } else {
            return ResumeVersion.reconstruct(
                    VERSION_ID, GROUP_ID, type, null, null,
                    "text/markdown", 0L, null, null,
                    "", null, ParseStatus.PENDING, null, ResumeVersion.Status.ACTIVE,
                    java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
            );
        }
    }
}
