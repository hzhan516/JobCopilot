package io.jobcopilot.resumeassistant.trigger.http.controller.resume;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import io.jobcopilot.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import io.jobcopilot.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import io.jobcopilot.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import io.jobcopilot.resumeassistant.api.resume.facade.ResumeFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeController 单元测试
 * ResumeController Unit Tests
 * <p>
 * 测试简历控制器：
 * Tests the resume controller:
 * - 上传处理
 * - Upload handling
 * - 下载处理
 * - Download handling
 * - CRUD 操作
 * - CRUD operations
 * - 响应包装
 * - Response wrapping
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Resume Controller Tests")
class ResumeControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ResumeFacade resumeFacade;

    @InjectMocks
    private ResumeController resumeController;

    private ResumeUploadResponse testUploadResponse;
    private ResumeGroupResponse testGroupResponse;
    private ResumeVersionResponse testVersionResponse;

    @BeforeEach
    void setUp() {
        testUploadResponse = ResumeUploadResponse.builder()
                .groupId(GROUP_ID)
                .originalVersionId(UUID.randomUUID())
                .title("Test Resume")
                .build();

        testGroupResponse = ResumeGroupResponse.builder()
                .groupId(GROUP_ID)
                .title("Test Resume")
                .build();

        testVersionResponse = ResumeVersionResponse.builder()
                .versionId(VERSION_ID)
                .groupId(GROUP_ID)
                .versionType("CONVERTED")
                .status("ACTIVE")
                .build();
    }

    // ==================== 上传测试 ====================
    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should upload resume and return response")
    void shouldUploadResumeAndReturnResponse() {
        // 给定
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");

        ApiResponse<ResumeUploadResponse> apiResponse = ApiResponse.success(testUploadResponse);
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<ResumeUploadResponse>> response =
                resumeController.uploadResume(mockFile, "My Resume", USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getGroupId()).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("Should pass file and title to facade on upload")
    void shouldPassFileAndTitleToFacadeOnUpload() {
        // 给定
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // 当
        // When
        resumeController.uploadResume(mockFile, "My Resume", USER_ID);

        // 那么
        // Then
        verify(resumeFacade).uploadResume(argThat(req ->
                req.getFile() == mockFile &&
                        req.getTitle().equals("My Resume")), eq(USER_ID));
    }

    @Test
    @DisplayName("Should handle null title on upload")
    void shouldHandleNullTitleOnUpload() {
        // 给定
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // 当
        // When
        resumeController.uploadResume(mockFile, null, USER_ID);

        // 那么
        // Then
        verify(resumeFacade).uploadResume(any(), eq(USER_ID));
    }

    // ==================== 下载测试 ====================
    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should download resume and return response entity")
    void shouldDownloadResumeAndReturnResponseEntity() {
        // 给定
        // Given
        InputStreamResource resource = new InputStreamResource(
                new ByteArrayInputStream("PDF content".getBytes()));
        ResponseEntity<InputStreamResource> expectedResponse = ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"resume.pdf\"")
                .body(resource);

        when(resumeFacade.downloadResume(VERSION_ID, USER_ID, "pdf"))
                .thenReturn(expectedResponse);

        // 当
        // When
        ResponseEntity<InputStreamResource> response =
                resumeController.downloadResume(VERSION_ID, USER_ID, "pdf");

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should pass parameters to facade on download")
    void shouldPassParametersToFacadeOnDownload() {
        // 给定
        // Given
        when(resumeFacade.downloadResume(any(), any(), any()))
                .thenReturn(ResponseEntity.ok().body(mock(InputStreamResource.class)));

        // 当
        // When
        resumeController.downloadResume(VERSION_ID, USER_ID, "docx");

        // 那么
        // Then
        verify(resumeFacade).downloadResume(VERSION_ID, USER_ID, "docx");
    }

    // ==================== 分组查询测试 ====================
    // ==================== Group Query Tests ====================

    @Test
    @DisplayName("Should get resume groups")
    void shouldGetResumeGroups() {
        // 给定
        // Given
        ApiResponse<List<ResumeGroupResponse>> apiResponse =
                ApiResponse.success(List.of(testGroupResponse));
        when(resumeFacade.getResumeGroups(USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> response =
                resumeController.getResumeGroups(USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("Should get single resume group")
    void shouldGetSingleResumeGroup() {
        // 给定
        // Given
        ApiResponse<ResumeGroupResponse> apiResponse = ApiResponse.success(testGroupResponse);
        when(resumeFacade.getResumeGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<ResumeGroupResponse>> response =
                resumeController.getResumeGroup(GROUP_ID, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getGroupId()).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("Should delete resume group")
    void shouldDeleteResumeGroup() {
        // 给定
        // Given
        ApiResponse<Void> apiResponse = ApiResponse.success(null);
        when(resumeFacade.deleteResumeGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<Void>> response =
                resumeController.deleteResumeGroup(GROUP_ID, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resumeFacade).deleteResumeGroup(GROUP_ID, USER_ID);
    }

    // ==================== 版本测试 ====================
    // ==================== Version Tests ====================

    @Test
    @DisplayName("Should get versions by group")
    void shouldGetVersionsByGroup() {
        // 给定
        // Given
        ApiResponse<List<ResumeVersionResponse>> apiResponse =
                ApiResponse.success(List.of(testVersionResponse));
        when(resumeFacade.getVersionsByGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<List<ResumeVersionResponse>>> response =
                resumeController.getVersionsByGroup(GROUP_ID, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("Should get single version")
    void shouldGetSingleVersion() {
        // 给定
        // Given
        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.getVersion(VERSION_ID, USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response =
                resumeController.getVersion(VERSION_ID, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getVersionId()).isEqualTo(VERSION_ID);
    }

    @Test
    @DisplayName("Should delete version")
    void shouldDeleteVersion() {
        // 给定
        // Given
        ApiResponse<Void> apiResponse = ApiResponse.success(null);
        when(resumeFacade.deleteVersion(VERSION_ID, USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<Void>> response =
                resumeController.deleteVersion(VERSION_ID, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resumeFacade).deleteVersion(VERSION_ID, USER_ID);
    }

    // ==================== 编辑测试 ====================
    // ==================== Edit Tests ====================

    @Test
    @DisplayName("Should edit version and return updated version")
    void shouldEditVersionAndReturnUpdatedVersion() {
        // 给定
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                .versionId(VERSION_ID)
                .content("Updated content")
                .build();

        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.editVersion(any(), eq(USER_ID))).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response =
                resumeController.editVersion(VERSION_ID, request, USER_ID);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should override version ID from path in edit request")
    void shouldOverrideVersionIdFromPathInEditRequest() {
        // 给定
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                // 与路径不同
                .versionId(UUID.randomUUID()) // Different from path
                .content("Updated content")
                .build();

        when(resumeFacade.editVersion(any(), eq(USER_ID)))
                .thenReturn(ApiResponse.success(testVersionResponse));

        // 当
        // When
        resumeController.editVersion(VERSION_ID, request, USER_ID);

        // 那么
        // Then
        verify(resumeFacade).editVersion(argThat(req ->
                req.getVersionId().equals(VERSION_ID)), eq(USER_ID));
    }

    @Test
    @DisplayName("Should create version and return new version")
    void shouldCreateVersionAndReturnNewVersion() {
        // 给定 / Given
        io.jobcopilot.resumeassistant.api.resume.dto.request.CreateVersionRequest request =
                io.jobcopilot.resumeassistant.api.resume.dto.request.CreateVersionRequest.builder()
                        .sourceVersionId(VERSION_ID)
                        .build();

        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.createVersion(eq(GROUP_ID), any(), eq(USER_ID))).thenReturn(apiResponse);

        // 当 / When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response =
                resumeController.createVersion(GROUP_ID, request, USER_ID);

        // 那么 / Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        verify(resumeFacade).createVersion(eq(GROUP_ID), any(), eq(USER_ID));
    }

    @Test
    @DisplayName("Should create version without source version id")
    void shouldCreateVersionWithoutSourceVersionId() {
        // 给定 / Given
        io.jobcopilot.resumeassistant.api.resume.dto.request.CreateVersionRequest request =
                io.jobcopilot.resumeassistant.api.resume.dto.request.CreateVersionRequest.builder()
                        .sourceVersionId(null)
                        .build();

        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.createVersion(eq(GROUP_ID), any(), eq(USER_ID))).thenReturn(apiResponse);

        // 当 / When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response =
                resumeController.createVersion(GROUP_ID, request, USER_ID);

        // 那么 / Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resumeFacade).createVersion(eq(GROUP_ID), argThat(req -> req.sourceVersionId() == null), eq(USER_ID));
    }

    @Test
    @DisplayName("Should activate version and return activated version")
    void shouldActivateVersionAndReturnActivatedVersion() {
        // 给定 / Given
        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.activateVersion(VERSION_ID, USER_ID)).thenReturn(apiResponse);

        // 当 / When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response =
                resumeController.activateVersion(VERSION_ID, USER_ID);

        // 那么 / Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        verify(resumeFacade).activateVersion(VERSION_ID, USER_ID);
    }

    @Test
    @DisplayName("Should handle empty groups list")
    void shouldHandleEmptyGroupsList() {
        // 给定
        // Given
        ApiResponse<List<ResumeGroupResponse>> apiResponse =
                ApiResponse.success(Collections.emptyList());
        when(resumeFacade.getResumeGroups(USER_ID)).thenReturn(apiResponse);

        // 当
        // When
        ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> response =
                resumeController.getResumeGroups(USER_ID);

        // 那么
        // Then
        assertThat(response.getBody().getData()).isEmpty();
    }
}
