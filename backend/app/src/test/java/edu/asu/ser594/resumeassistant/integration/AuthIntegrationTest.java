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
 * Auth Integration Tests
 * 
 * Integration tests for authentication flow:
 * - End-to-end registration
 * - End-to-end login
 * - Token generation
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
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = restTemplate.exchange(
                RequestEntity.post(URI.create(BASE_URL + "/register/email"))
                        .body(request),
                new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

        // When - Try to register again
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/register/email",
                request,
                AuthResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email("nonexistent@test.com")
                .password("wrongpassword")
                .build();

        // When
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login/email",
                request,
                AuthResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
