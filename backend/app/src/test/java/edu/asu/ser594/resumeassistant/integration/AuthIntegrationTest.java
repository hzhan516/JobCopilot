package edu.asu.ser594.resumeassistant.integration;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
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
 * 身份验证集成测试
 * Auth Integration Tests
 * 
 * 身份验证流程的集成测试：
 * Integration tests for authentication flow:
 * - 端到端注册
 * - End-to-end registration
 * - 端到端登录
 * - End-to-end login
 * - 代币生成
 * - Token generation
 * - 错误处理
 * - Error handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String BASE_URL = "/v1/auth";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should complete registration flow successfully")
    void shouldCompleteRegistrationFlowSuccessfully() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        // 什么时候
        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                RequestEntity.post(URI.create(BASE_URL + "/register/email"))
                        .body(request),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        // 然后
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(response.getBody().getData().getUserId()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isNotNull();
        assertThat(response.getBody().getData().getRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("Should complete login flow successfully after registration")
    void shouldCompleteLoginFlowSuccessfullyAfterRegistration() {
        // 给定 - 先注册
        // Given - Register first
        RegisterByEmailRequest registerRequest = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL + ".login")
                .password(TEST_PASSWORD)
                .build();

        restTemplate.exchange(
                RequestEntity.post(URI.create(BASE_URL + "/register/email"))
                        .body(registerRequest),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        // 何时 - 登录
        // When - Login
        LoginByEmailRequest loginRequest = LoginByEmailRequest.builder()
                .email(TEST_EMAIL + ".login")
                .password(TEST_PASSWORD)
                .build();

        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                RequestEntity.post(URI.create(BASE_URL + "/login/email"))
                        .body(loginRequest),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        // 然后
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL + ".login");
        assertThat(response.getBody().getData().getAccessToken()).isNotNull();
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void shouldRejectRegistrationWithDuplicateEmail() {
        // 给定 - 先注册
        // Given - Register first
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL + ".dup")
                .password(TEST_PASSWORD)
                .build();

        restTemplate.postForEntity(
                BASE_URL + "/register/email",
                request,
                AuthResponse.class
        );

        // 何时 - 尝试再次注册
        // When - Try to register again
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/register/email",
                request,
                AuthResponse.class
        );

        // 然后
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("nonexistent@test.com")
                .password("wrongpassword")
                .build();

        // 什么时候
        // When
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login/email",
                request,
                AuthResponse.class
        );

        // 然后
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
