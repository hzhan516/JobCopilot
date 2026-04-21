package edu.asu.ser594.resumeassistant.trigger.http.controller.resume;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResumeController Unit Tests
 * 
 * Tests the resume controller:
 * - Upload handling
 * - Download handling
 * - CRUD operations
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

    // ==================== Upload Tests ====================

    @Test
    @DisplayName("Should upload resume and return response")
    void shouldUploadResumeAndReturnResponse() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");

        ApiResponse<ResumeUploadResponse> apiResponse = ApiResponse.success(testUploadResponse);
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // When
        ResponseEntity<ApiResponse<ResumeUploadResponse>> response = 
                resumeController.uploadResume(mockFile, "My Resume", USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getGroupId()).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("Should pass file and title to facade on upload")
    void shouldPassFileAndTitleToFacadeOnUpload() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // When
        resumeController.uploadResume(mockFile, "My Resume", USER_ID);

        // Then
        verify(resumeFacade).uploadResume(argThat(req ->
                req.getFile() == mockFile &&
                        req.getTitle().equals("My Resume")), eq(USER_ID));
    }

    @Test
    @DisplayName("Should handle null title on upload")
    void shouldHandleNullTitleOnUpload() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("resume.pdf");
        when(resumeFacade.uploadResume(any(), eq(USER_ID))).thenReturn(testUploadResponse);

        // When
        resumeController.uploadResume(mockFile, null, USER_ID);

        // Then
        verify(resumeFacade).uploadResume(any(), eq(USER_ID));
    }

    // ==================== Download Tests ====================

    @Test
    @DisplayName("Should download resume and return response entity")
    void shouldDownloadResumeAndReturnResponseEntity() {
        // Given
        InputStreamResource resource = new InputStreamResource(
                new ByteArrayInputStream("PDF content".getBytes()));
        ResponseEntity<InputStreamResource> expectedResponse = ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"resume.pdf\"")
                .body(resource);

        when(resumeFacade.downloadResume(VERSION_ID, USER_ID, "pdf"))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<InputStreamResource> response = 
                resumeController.downloadResume(VERSION_ID, USER_ID, "pdf");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should pass parameters to facade on download")
    void shouldPassParametersToFacadeOnDownload() {
        // Given
        when(resumeFacade.downloadResume(any(), any(), any()))
                .thenReturn(ResponseEntity.ok().body(mock(InputStreamResource.class)));

        // When
        resumeController.downloadResume(VERSION_ID, USER_ID, "docx");

        // Then
        verify(resumeFacade).downloadResume(VERSION_ID, USER_ID, "docx");
    }

    // ==================== Group Query Tests ====================

    @Test
    @DisplayName("Should get resume groups")
    void shouldGetResumeGroups() {
        // Given
        ApiResponse<List<ResumeGroupResponse>> apiResponse = 
                ApiResponse.success(List.of(testGroupResponse));
        when(resumeFacade.getResumeGroups(USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> response = 
                resumeController.getResumeGroups(USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("Should get single resume group")
    void shouldGetSingleResumeGroup() {
        // Given
        ApiResponse<ResumeGroupResponse> apiResponse = ApiResponse.success(testGroupResponse);
        when(resumeFacade.getResumeGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<ResumeGroupResponse>> response = 
                resumeController.getResumeGroup(GROUP_ID, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getGroupId()).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("Should delete resume group")
    void shouldDeleteResumeGroup() {
        // Given
        ApiResponse<Void> apiResponse = ApiResponse.success(null);
        when(resumeFacade.deleteResumeGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<Void>> response = 
                resumeController.deleteResumeGroup(GROUP_ID, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resumeFacade).deleteResumeGroup(GROUP_ID, USER_ID);
    }

    // ==================== Version Tests ====================

    @Test
    @DisplayName("Should get versions by group")
    void shouldGetVersionsByGroup() {
        // Given
        ApiResponse<List<ResumeVersionResponse>> apiResponse = 
                ApiResponse.success(List.of(testVersionResponse));
        when(resumeFacade.getVersionsByGroup(GROUP_ID, USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<List<ResumeVersionResponse>>> response = 
                resumeController.getVersionsByGroup(GROUP_ID, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }

    @Test
    @DisplayName("Should get single version")
    void shouldGetSingleVersion() {
        // Given
        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.getVersion(VERSION_ID, USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response = 
                resumeController.getVersion(VERSION_ID, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getVersionId()).isEqualTo(VERSION_ID);
    }

    @Test
    @DisplayName("Should delete version")
    void shouldDeleteVersion() {
        // Given
        ApiResponse<Void> apiResponse = ApiResponse.success(null);
        when(resumeFacade.deleteVersion(VERSION_ID, USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<Void>> response = 
                resumeController.deleteVersion(VERSION_ID, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resumeFacade).deleteVersion(VERSION_ID, USER_ID);
    }

    // ==================== Edit Tests ====================

    @Test
    @DisplayName("Should edit version and return updated version")
    void shouldEditVersionAndReturnUpdatedVersion() {
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                .versionId(VERSION_ID)
                .content("Updated content")
                .build();

        ApiResponse<ResumeVersionResponse> apiResponse = ApiResponse.success(testVersionResponse);
        when(resumeFacade.editVersion(any(), eq(USER_ID))).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<ResumeVersionResponse>> response = 
                resumeController.editVersion(VERSION_ID, request, USER_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should override version ID from path in edit request")
    void shouldOverrideVersionIdFromPathInEditRequest() {
        // Given
        ResumeEditRequest request = ResumeEditRequest.builder()
                .versionId(UUID.randomUUID()) // Different from path
                .content("Updated content")
                .build();

        when(resumeFacade.editVersion(any(), eq(USER_ID)))
                .thenReturn(ApiResponse.success(testVersionResponse));

        // When
        resumeController.editVersion(VERSION_ID, request, USER_ID);

        // Then
        verify(resumeFacade).editVersion(argThat(req ->
                req.getVersionId().equals(VERSION_ID)), eq(USER_ID));
    }

    @Test
    @DisplayName("Should handle empty groups list")
    void shouldHandleEmptyGroupsList() {
        // Given
        ApiResponse<List<ResumeGroupResponse>> apiResponse = 
                ApiResponse.success(Collections.emptyList());
        when(resumeFacade.getResumeGroups(USER_ID)).thenReturn(apiResponse);

        // When
        ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> response = 
                resumeController.getResumeGroups(USER_ID);

        // Then
        assertThat(response.getBody().getData()).isEmpty();
    }
}
