package io.jobcopilot.resumeassistant.trigger.http.controller.user;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResponse;
import io.jobcopilot.resumeassistant.api.user.dto.response.AuthResult;
import io.jobcopilot.resumeassistant.api.user.facade.AuthFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthController 单元测试
 * AuthController Unit Tests
 * <p>
 * 测试认证控制器：
 * Tests the authentication controller:
 * - 请求处理
 * - Request handling
 * - 响应包装
 * - Response wrapping
 * - 委托给门面层
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

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController authController;

    private AuthResponse testAuthResponse;
    private AuthResult testAuthResult;

    @BeforeEach
    void setUp() {
        testAuthResponse = AuthResponse.builder()
                .userId(USER_ID)
                .email(TEST_EMAIL)
                .accessToken("access-token")
                .expiresIn(3600L)
                .build();
        testAuthResult = new AuthResult(testAuthResponse, "refresh-token");
        when(httpRequest.isSecure()).thenReturn(false);
    }

    // ==================== 注册测试 ====================
    // ==================== Register Tests ====================

    @Test
    @DisplayName("Should register user and return success response")
    void shouldRegisterUserAndReturnSuccessResponse() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any(RegisterByEmailRequest.class)))
                .thenReturn(testAuthResult);

        // 当
        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.registerByEmail(request, httpRequest);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should pass request to facade during registration")
    void shouldPassRequestToFacadeDuringRegistration() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResult);

        // 当
        // When
        authController.registerByEmail(request, httpRequest);

        // 那么
        // Then
        verify(authFacade).registerByEmail(argThat(req ->
                req.getEmail().equals(TEST_EMAIL) &&
                        req.getPassword().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should wrap response in ApiResponse on registration")
    void shouldWrapResponseInApiResponseOnRegistration() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResult);

        // 当
        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.registerByEmail(request, httpRequest);

        // 那么
        // Then
        ApiResponse<AuthResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode() == 200).isTrue();
        assertThat(body.getData()).isEqualTo(testAuthResponse);
        assertThat(body.getCode()).isEqualTo(200);
    }

    // ==================== 登录测试 ====================
    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should login user and return success response")
    void shouldLoginUserAndReturnSuccessResponse() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any(LoginByEmailRequest.class)))
                .thenReturn(testAuthResult);

        // 当
        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.loginByEmail(request, httpRequest);

        // 那么
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode() == 200).isTrue();
        assertThat(response.getBody().getData().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should pass request to facade during login")
    void shouldPassRequestToFacadeDuringLogin() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any())).thenReturn(testAuthResult);

        // 当
        // When
        authController.loginByEmail(request, httpRequest);

        // 那么
        // Then
        verify(authFacade).loginByEmail(argThat(req ->
                req.getEmail().equals(TEST_EMAIL) &&
                        req.getPassword().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should include tokens in successful login response")
    void shouldIncludeTokensInSuccessfulLoginResponse() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.loginByEmail(any())).thenReturn(testAuthResult);

        // 当
        // When
        ResponseEntity<ApiResponse<AuthResponse>> response = authController.loginByEmail(request, httpRequest);

        // 那么
        // Then
        AuthResponse data = response.getBody().getData();
        assertThat(data.getAccessToken()).isEqualTo("access-token");
        assertThat(data.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refreshToken=refresh-token");
    }

    @Test
    @DisplayName("Should return OK status for both endpoints")
    void shouldReturnOkStatusForBothEndpoints() {
        // 给定
        // Given
        RegisterByEmailRequest registerRequest = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        LoginByEmailRequest loginRequest = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authFacade.registerByEmail(any())).thenReturn(testAuthResult);
        when(authFacade.loginByEmail(any())).thenReturn(testAuthResult);

        // 当
        // When
        ResponseEntity<ApiResponse<AuthResponse>> registerResponse = authController.registerByEmail(registerRequest, httpRequest);
        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = authController.loginByEmail(loginRequest, httpRequest);

        // 那么
        // Then
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
