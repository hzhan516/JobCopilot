package io.jobcopilot.resumeassistant.integration;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.resume.dto.request.CreateVersionRequest;
import io.jobcopilot.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import io.jobcopilot.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简历版本集成测试
 * Resume Version Integration Tests
 * <p>
 * 测试简历版本创建副本的端到端流程：
 * Tests the end-to-end flow for creating resume version copies:
 * - 上传简历后创建副本
 * - Create copy after uploading resume
 * - 副本内容正确继承
 * - Copy correctly inherits content
 * - 版本链归档行为
 * - Version chain archiving behavior
 */
@Transactional
@DisplayName("Resume Version Integration Tests")
class ResumeVersionIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_EMAIL = "resume-version-test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String AUTH_BASE_URL = "/v1/auth";
    private static final String RESUME_BASE_URL = "/v1/resumes";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should create version copy after upload")
    void shouldCreateVersionCopyAfterUpload() {
        // 给定 - 注册、登录并上传简历
        // Given - Register, login and upload resume
        String token = registerAndGetToken();
        UUID groupId = uploadResumeAndGetGroupId(token);

        // 当 - 创建版本副本
        // When - Create version copy
        CreateVersionRequest request = CreateVersionRequest.builder()
                .sourceVersionId(null)
                .build();

        ResponseEntity<ApiResponse<ResumeVersionResponse>> response = restTemplate.exchange(
                RequestEntity.post(URI.create(RESUME_BASE_URL + "/groups/" + groupId + "/versions"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么 - 验证响应
        // Then - Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getVersionType()).isEqualTo("CONVERTED");
        assertThat(response.getBody().getData().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should archive old converted when creating new copy")
    void shouldArchiveOldConvertedWhenCreatingNewCopy() {
        // 给定 - 注册、登录、上传简历并创建第一个副本
        // Given - Register, login, upload resume and create first copy
        String token = registerAndGetToken();
        UUID groupId = uploadResumeAndGetGroupId(token);

        CreateVersionRequest request = CreateVersionRequest.builder().sourceVersionId(null).build();
        ResponseEntity<ApiResponse<ResumeVersionResponse>> firstCopy = restTemplate.exchange(
                RequestEntity.post(URI.create(RESUME_BASE_URL + "/groups/" + groupId + "/versions"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(firstCopy.getBody()).isNotNull();
        UUID firstVersionId = firstCopy.getBody().getData().getVersionId();

        // 当 - 创建第二个副本
        // When - Create second copy
        ResponseEntity<ApiResponse<ResumeVersionResponse>> secondCopy = restTemplate.exchange(
                RequestEntity.post(URI.create(RESUME_BASE_URL + "/groups/" + groupId + "/versions"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么 - 第二个副本应为 ACTIVE，第一个应被归档
        // Then - Second copy should be ACTIVE, first should be archived
        assertThat(secondCopy.getBody()).isNotNull();
        assertThat(secondCopy.getBody().getData().getStatus()).isEqualTo("ACTIVE");

        // 查询版本列表验证
        // Query version list to verify
        ResponseEntity<ApiResponse<java.util.List<ResumeVersionResponse>>> versionsResponse = restTemplate.exchange(
                RequestEntity.get(URI.create(RESUME_BASE_URL + "/groups/" + groupId + "/versions"))
                        .header("Authorization", "Bearer " + token)
                        .build(),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(versionsResponse.getBody()).isNotNull();
        java.util.List<ResumeVersionResponse> versions = versionsResponse.getBody().getData();
        assertThat(versions).anyMatch(v -> v.getVersionId().equals(firstVersionId) && v.getStatus().equals("ARCHIVED"));
        assertThat(versions).anyMatch(v -> v.getVersionId().equals(secondCopy.getBody().getData().getVersionId()) && v.getStatus().equals("ACTIVE"));
    }

    // 辅助方法：注册并获取Token / Helper: register and get token
    private String registerAndGetToken() {
        String email = TEST_EMAIL + "." + System.nanoTime();
        RegisterByEmailRequest registerRequest = RegisterByEmailRequest.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .build();

        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                RequestEntity.post(URI.create(AUTH_BASE_URL + "/register/email"))
                        .body(registerRequest),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        return response.getBody().getData().getAccessToken();
    }

    // 辅助方法：上传简历并获取组ID / Helper: upload resume and get group ID
    private UUID uploadResumeAndGetGroupId(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test pdf content".getBytes()) {
            @Override
            public String getFilename() {
                return "resume.pdf";
            }
        });
        body.add("title", "Test Resume");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponse<ResumeUploadResponse>> response = restTemplate.exchange(
                RESUME_BASE_URL,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        return response.getBody().getData().getGroupId();
    }
}
