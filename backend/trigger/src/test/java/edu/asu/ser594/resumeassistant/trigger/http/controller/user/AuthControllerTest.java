package edu.asu.ser594.resumeassistant.trigger.http.controller.user;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.AuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthController Unit Tests
 * 
 * Tests the authentication controller:
 * - Request handling
 * - Response wrapping
 * - Delegation to facade
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private AuthController authController;

    private AuthResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        testAuthResponse = AuthResponse.builder()
                .userId(USER_ID)
                .email(TEST_EMAIL)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();
    }

    // ==================== Register Tests ====================

    @Test
    @DisplayName("Should register user and return success response")
    void shouldRegisterUserAndReturnSuccessResponse() {
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any(RegisterByEmailRequest.class)))
                .thenReturn(testAuthResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.registerByEmail(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should pass request to facade during registration")
    void shouldPassRequestToFacadeDuringRegistration() {
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResponse);

        // When
        authController.registerByEmail(request);

        // Then
        verify(authFacade).registerByEmail(argThat(req ->
                req.getEmail().equals(TEST_EMAIL) &&
                        req.getPassword().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should wrap response in ApiResponse on registration")
    void shouldWrapResponseInApiResponseOnRegistration() {
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.registerByEmail(request);

        // Then
        ApiResponse<AuthResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode() == 200).isTrue();
        assertThat(body.getData()).isEqualTo(testAuthResponse);
        assertThat(body.getCode()).isEqualTo(200);
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should login user and return success response")
    void shouldLoginUserAndReturnSuccessResponse() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any(LoginByEmailRequest.class)))
                .thenReturn(testAuthResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.loginByEmail(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should pass request to facade during login")
    void shouldPassRequestToFacadeDuringLogin() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any())).thenReturn(testAuthResponse);

        // When
        authController.loginByEmail(request);

        // Then
        verify(authFacade).loginByEmail(argThat(req ->
                req.getEmail().equals(TEST_EMAIL) &&
                        req.getPassword().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should include tokens in successful login response")
    void shouldIncludeTokensInSuccessfulLoginResponse() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any())).thenReturn(testAuthResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.loginByEmail(request);

        // Then
        AuthResponse data = response.getBody().getData();
        assertThat(data.getAccessToken()).isEqualTo("access-token");
        assertThat(data.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(data.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should return OK status for both endpoints")
    void shouldReturnOkStatusForBothEndpoints() {
        // Given
        RegisterByEmailRequest registerRequest = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        LoginByEmailRequest loginRequest = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResponse);
        when(authFacade.loginByEmail(any())).thenReturn(testAuthResponse);

        // When
        ResponseEntity<ApiResponse<AuthResponse>> registerResponse = authController.registerByEmail(registerRequest);
        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = authController.loginByEmail(loginRequest);

        // Then
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
