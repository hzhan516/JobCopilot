package edu.asu.ser594.resumeassistant.application.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.application.resume.command.CreateVersionCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
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
    @DisplayName("Should activate archived version")
    void shouldActivateArchivedVersion() {
        // 准备 / Given
        UUID archivedId = UUID.randomUUID();
        ResumeVersion archivedVersion = ResumeVersion.reconstruct(
                archivedId, GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "Archived content", null, ParseStatus.PENDING, null,
                ResumeVersion.Status.ARCHIVED, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        testGroup = createTestGroup();
        testGroup.addVersion(archivedVersion);

        when(versionRepository.findById(archivedId)).thenReturn(Optional.of(archivedVersion));
        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
        doNothing().when(groupRepository).save(any(ResumeGroup.class));

        // 执行 / When
        ResumeVersion result = resumeService.handleActivateVersion(archivedId, USER_ID);

        // 验证 / Then
        assertThat(result.getStatus()).isEqualTo(ResumeVersion.Status.ACTIVE);
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
