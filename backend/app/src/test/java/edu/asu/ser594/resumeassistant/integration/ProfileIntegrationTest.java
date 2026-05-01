package edu.asu.ser594.resumeassistant.integration;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.response.ProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户资料集成测试
 * Profile Integration Tests
 * <p>
 * 用户资料端到端流程的集成测试：
 * Integration tests for profile end-to-end flow:
 * - 注册后自动创建空资料
 * - Auto-create empty profile after registration
 * - 获取资料
 * - Get profile
 * - 更新资料
 * - Update profile
 * - 更新头像
 * - Update avatar
 * - 未认证访问被拒绝
 * - Unauthenticated access rejected
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Profile Integration Tests")
class ProfileIntegrationTest {

    private static final String TEST_EMAIL = "profile-test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String AUTH_BASE_URL = "/v1/auth";
    private static final String PROFILE_BASE_URL = "/v1/profile";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should get profile after registration")
    void shouldGetProfileAfterRegistration() {
        // 给定 - 注册并获取token
        // Given - Register and get token
        String token = registerAndGetToken();

        // 当 - 获取用户资料
        // When - Get user profile
        ResponseEntity<ApiResponse<ProfileResponse>> response = restTemplate.exchange(
                RequestEntity.get(URI.create(PROFILE_BASE_URL))
                        .header("Authorization", "Bearer " + token)
                        .build(),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().userId()).isNotNull();
        assertThat(response.getBody().getData().fullName()).isNull();
        assertThat(response.getBody().getData().avatarUrl()).isNull();
        assertThat(response.getBody().getData().phone()).isNull();
        assertThat(response.getBody().getData().targetPosition()).isNull();
        assertThat(response.getBody().getData().preferredLocation()).isNull();
        assertThat(response.getBody().getData().createdAt()).isNotNull();
        assertThat(response.getBody().getData().updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update profile successfully")
    void shouldUpdateProfileSuccessfully() {
        // 给定 - 注册并获取token
        // Given - Register and get token
        String token = registerAndGetToken();

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Alice Zhang",
                "+1-555-0199",
                "Software Engineer",
                "San Francisco, CA"
        );

        // 当 - 更新用户资料
        // When - Update user profile
        ResponseEntity<ApiResponse<ProfileResponse>> response = restTemplate.exchange(
                RequestEntity.put(URI.create(PROFILE_BASE_URL))
                        .header("Authorization", "Bearer " + token)
                        .body(request),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().fullName()).isEqualTo("Alice Zhang");
        assertThat(response.getBody().getData().phone()).isEqualTo("+1-555-0199");
        assertThat(response.getBody().getData().targetPosition()).isEqualTo("Software Engineer");
        assertThat(response.getBody().getData().preferredLocation()).isEqualTo("San Francisco, CA");
    }

    @Test
    @DisplayName("Should update avatar successfully")
    void shouldUpdateAvatarSuccessfully() {
        // 给定 - 注册并获取token
        // Given - Register and get token
        String token = registerAndGetToken();

        UpdateAvatarRequest request = new UpdateAvatarRequest("https://storage.example.com/avatar.png");

        // 当 - 更新头像
        // When - Update avatar
        ResponseEntity<ApiResponse<ProfileResponse>> response = restTemplate.exchange(
                RequestEntity.put(URI.create(PROFILE_BASE_URL + "/avatar"))
                        .header("Authorization", "Bearer " + token)
                        .body(request),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().avatarUrl()).isEqualTo("https://storage.example.com/avatar.png");
    }

    @Test
    @DisplayName("Should reject unauthenticated profile access")
    void shouldRejectUnauthenticatedProfileAccess() {
        // 当 - 未携带token请求资料
        // When - Request profile without token
        ResponseEntity<ApiResponse<ProfileResponse>> response = restTemplate.exchange(
                RequestEntity.get(URI.create(PROFILE_BASE_URL)).build(),
                new ParameterizedTypeReference<>() {
                }
        );

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * 辅助方法：注册用户并获取accessToken
     * Helper: register user and get access token
     */
    private String registerAndGetToken() {
        String email = TEST_EMAIL + "." + System.nanoTime();
        RegisterByEmailRequest registerRequest = RegisterByEmailRequest.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .build();

        ResponseEntity<ApiResponse<AuthResponse>> registerResponse = restTemplate.exchange(
                RequestEntity.post(URI.create(AUTH_BASE_URL + "/register/email"))
                        .body(registerRequest),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getData()).isNotNull();
        return registerResponse.getBody().getData().getAccessToken();
    }
}
