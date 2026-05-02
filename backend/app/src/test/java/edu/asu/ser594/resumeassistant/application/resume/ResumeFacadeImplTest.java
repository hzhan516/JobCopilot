package edu.asu.ser594.resumeassistant.application.resume;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import edu.asu.ser594.resumeassistant.application.resume.command.CreateVersionCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.application.resume.service.ResumeApplicationService;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ResumeFacadeImpl 单元测试
 * ResumeFacadeImpl Unit Tests
 * <p>
 * 按照 DDD 反腐败层模式测试简历外观实现：
 * Tests the resume facade implementation following DDD anti-corruption layer patterns:
 * - 请求到命令的转换
 * - Request to Command conversion
 * - 响应组装
 * - Response assembly
 * - 文件处理
 * - File handling
 * - 下载响应构建
 * - Download response building
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Resume Facade Implementation Tests")
class ResumeFacadeImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ResumeApplicationService resumeService;

    @InjectMocks
    private ResumeFacadeImpl resumeFacade;

    private ResumeGroup testGroup;

    @BeforeEach
    void setUp() {
        testGroup = ResumeGroup.create(USER_ID, "Test Resume");
    }

    // ====================上传测试====================
    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should upload resume and return response")
    void shouldUploadResumeAndReturnResponse() throws IOException {
        // 给定
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenReturn(inputStream);

        ResumeUploadRequest request = ResumeUploadRequest.builder()
                .file(mockFile)
                .title("My Resume")
                .build();

        when(resumeService.handleUpload(any(ResumeUploadCommand.class), eq(USER_ID)))
                .thenReturn(testGroup);

        // 什么时候
        // When
        ResumeUploadResponse result = resumeFacade.uploadResume(request, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getGroupId()).isEqualTo(testGroup.getId());
        verify(resumeService).handleUpload(any(ResumeUploadCommand.class), eq(USER_ID));
    }

    @Test
    @DisplayName("Should convert file correctly in upload command")
    void shouldConvertFileCorrectlyInUploadCommand() throws IOException {
        // 给定
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());

        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getInputStream()).thenReturn(inputStream);

        ResumeUploadRequest request = ResumeUploadRequest.builder()
                .file(mockFile)
                .title("My Resume")
                .build();

        ArgumentCaptor<ResumeUploadCommand> commandCaptor = ArgumentCaptor.forClass(ResumeUploadCommand.class);
        when(resumeService.handleUpload(commandCaptor.capture(), eq(USER_ID))).thenReturn(testGroup);

        // 什么时候
        // When
        resumeFacade.uploadResume(request, USER_ID);

        // 然后
        // Then
        ResumeUploadCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.fileName()).isEqualTo("resume.pdf");
        assertThat(capturedCommand.contentType()).isEqualTo("application/pdf");
        assertThat(capturedCommand.fileSize()).isEqualTo(1024L);
        assertThat(capturedCommand.title()).isEqualTo("My Resume");
    }

    // ==================== 下载测试 ====================
    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should download resume and return response entity")
    void shouldDownloadResumeAndReturnResponseEntity() {
        // 给定
        // Given
        InputStream inputStream = new ByteArrayInputStream("PDF content".getBytes());
        ResumeDownloadResult downloadResult = ResumeDownloadResult.builder()
                .inputStream(inputStream)
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .build();

        when(resumeService.handleDownload(any(ResumeDownloadQuery.class))).thenReturn(downloadResult);

        // 什么时候
        // When
        ResponseEntity<InputStreamResource> result = resumeFacade.downloadResume(VERSION_ID, USER_ID, "pdf");

        // 然后
        // Then
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("resume.pdf");
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
    }

    @Test
    @DisplayName("Should pass correct query to service during download")
    void shouldPassCorrectQueryToServiceDuringDownload() {
        // 给定
        // Given
        InputStream inputStream = new ByteArrayInputStream("content".getBytes());
        ResumeDownloadResult downloadResult = ResumeDownloadResult.builder()
                .inputStream(inputStream)
                .fileName("resume.pdf")
                .contentType("application/pdf")
                .build();

        ArgumentCaptor<ResumeDownloadQuery> queryCaptor = ArgumentCaptor.forClass(ResumeDownloadQuery.class);
        when(resumeService.handleDownload(queryCaptor.capture())).thenReturn(downloadResult);

        // 什么时候
        // When
        resumeFacade.downloadResume(VERSION_ID, USER_ID, "pdf");

        // 然后
        // Then
        ResumeDownloadQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.versionId()).isEqualTo(VERSION_ID);
        assertThat(capturedQuery.userId()).isEqualTo(USER_ID);
        assertThat(capturedQuery.targetFormat()).isEqualTo("pdf");
    }

    // ==================== 分组查询测试 ====================
    // ==================== Group Query Tests ====================

    @Test
    @DisplayName("Should get resume groups and return API response")
    void shouldGetResumeGroupsAndReturnApiResponse() {
        // 给定
        // Given
        when(resumeService.listUserGroups(USER_ID)).thenReturn(List.of(testGroup));

        // 什么时候
        // When
        ApiResponse<List<ResumeGroupResponse>> result = resumeFacade.getResumeGroups(USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getGroupId()).isEqualTo(testGroup.getId());
    }

    @Test
    @DisplayName("Should get single resume group")
    void shouldGetSingleResumeGroup() {
        // 给定
        // Given
        when(resumeService.getGroup(GROUP_ID, USER_ID)).thenReturn(testGroup);

        // 什么时候
        // When
        ApiResponse<ResumeGroupResponse> result = resumeFacade.getResumeGroup(GROUP_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        assertThat(result.getData().getGroupId()).isEqualTo(testGroup.getId());
    }

    @Test
    @DisplayName("Should delete resume group")
    void shouldDeleteResumeGroup() {
        // 什么时候
        // When
        ApiResponse<Void> result = resumeFacade.deleteResumeGroup(GROUP_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        verify(resumeService).handleDelete(GROUP_ID, USER_ID);
    }

    // ==================== 版本测试 ====================
    // ==================== Version Tests ====================

    @Test
    @DisplayName("Should get versions by group")
    void shouldGetVersionsByGroup() {
        // 给定
        // Given
        when(resumeService.getGroup(GROUP_ID, USER_ID)).thenReturn(testGroup);

        // 什么时候
        // When
        ApiResponse<List<ResumeVersionResponse>> result = resumeFacade.getVersionsByGroup(GROUP_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        verify(resumeService).getGroup(GROUP_ID, USER_ID);
    }

    @Test
    @DisplayName("Should get single version")
    void shouldGetSingleVersion() {
        // 给定
        // Given
        ResumeVersion version = ResumeVersion.createConverted(GROUP_ID);
        when(resumeService.getVersion(VERSION_ID, USER_ID)).thenReturn(version);

        // 什么时候
        // When
        ApiResponse<ResumeVersionResponse> result = resumeFacade.getVersion(VERSION_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        assertThat(result.getData().getVersionId()).isEqualTo(version.getId());
    }

    @Test
    @DisplayName("Should delete version")
    void shouldDeleteVersion() {
        // 什么时候
        // When
        ApiResponse<Void> result = resumeFacade.deleteVersion(VERSION_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        verify(resumeService).handleDeleteVersion(VERSION_ID, USER_ID);
    }

    // ==================== 编辑测试 ====================
    // ==================== Edit Tests ====================

    @Test
    @DisplayName("Should edit version and return response")
    void shouldEditVersionAndReturnResponse() {
        // 给定
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                .versionId(VERSION_ID)
                .content("Updated content")
                .build();

        ResumeVersion version = ResumeVersion.createConverted(GROUP_ID);
        version.editContent("Updated content");

        when(resumeService.handleEdit(any(ResumeEditCommand.class))).thenReturn(version);

        // 什么时候
        // When
        ApiResponse<ResumeVersionResponse> result = resumeFacade.editVersion(request, USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
    }

    @Test
    @DisplayName("Should convert request to command correctly for edit")
    void shouldConvertRequestToCommandCorrectlyForEdit() {
        // 给定
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                .versionId(VERSION_ID)
                .content("Updated content")
                .build();

        ResumeVersion version = ResumeVersion.createConverted(GROUP_ID);
        ArgumentCaptor<ResumeEditCommand> commandCaptor = ArgumentCaptor.forClass(ResumeEditCommand.class);
        when(resumeService.handleEdit(commandCaptor.capture())).thenReturn(version);

        // 什么时候
        // When
        resumeFacade.editVersion(request, USER_ID);

        // 然后
        // Then
        ResumeEditCommand capturedCommand = commandCaptor.getValue();
        assertThat(capturedCommand.versionId()).isEqualTo(VERSION_ID);
        assertThat(capturedCommand.userId()).isEqualTo(USER_ID);
        assertThat(capturedCommand.content()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("Should create version and return response")
    void shouldCreateVersionAndReturnResponse() {
        // 给定 / Given
        edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest request =
                edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest.builder()
                        .sourceVersionId(VERSION_ID)
                        .build();

        ResumeVersion newVersion = ResumeVersion.createConverted(GROUP_ID);
        newVersion.editContent("Copied content");

        when(resumeService.handleCreateVersion(any(CreateVersionCommand.class))).thenReturn(newVersion);

        // 什么时候 / When
        ApiResponse<ResumeVersionResponse> result = resumeFacade.createVersion(GROUP_ID, request, USER_ID);

        // 然后 / Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        assertThat(result.getData().getVersionId()).isEqualTo(newVersion.getId());
        assertThat(result.getData().getContent()).isEqualTo("Copied content");
    }

    @Test
    @DisplayName("Should convert create request to command correctly")
    void shouldConvertCreateRequestToCommandCorrectly() {
        // 给定 / Given
        UUID sourceId = UUID.randomUUID();
        edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest request =
                edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest.builder()
                        .sourceVersionId(sourceId)
                        .build();

        ResumeVersion newVersion = ResumeVersion.createConverted(GROUP_ID);
        ArgumentCaptor<CreateVersionCommand> commandCaptor = ArgumentCaptor.forClass(CreateVersionCommand.class);
        when(resumeService.handleCreateVersion(commandCaptor.capture())).thenReturn(newVersion);

        // 什么时候 / When
        resumeFacade.createVersion(GROUP_ID, request, USER_ID);

        // 然后 / Then
        CreateVersionCommand captured = commandCaptor.getValue();
        assertThat(captured.groupId()).isEqualTo(GROUP_ID);
        assertThat(captured.sourceVersionId()).isEqualTo(sourceId);
        assertThat(captured.userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should handle empty groups list")
    void shouldHandleEmptyGroupsList() {
        // 给定
        // Given
        when(resumeService.listUserGroups(USER_ID)).thenReturn(Collections.emptyList());

        // 什么时候
        // When
        ApiResponse<List<ResumeGroupResponse>> result = resumeFacade.getResumeGroups(USER_ID);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCode() == 200).isTrue();
        assertThat(result.getData()).isEmpty();
    }
}
