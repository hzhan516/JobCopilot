package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.LoginByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.RegisterByEmailRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.AuthResponse;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.application.user.service.AuthApplicationService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthFacadeImpl 单元测试
 * AuthFacadeImpl Unit Tests
 * 
 * 测试充当反腐败层的外观实现：
 * Tests the facade implementation that acts as an anti-corruption layer:
 * - DTO 到命令的转换
 * - DTO to Command conversion
 * - 响应组装
 * - Response assembly
 * - 与应用程序服务协调
 * - Coordination with application service
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Facade Implementation Tests")
class AuthFacadeImplTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";

    @Mock
    private AuthApplicationService authService;

    @InjectMocks
    private AuthFacadeImpl authFacade;

    private User testUser;
    private TokenPair testTokenPair;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .emailVerified(false)
                .role(UserRole.JOB_SEEKER)
                .status(UserStatus.ACTIVE)
                .build();

        testTokenPair = TokenPair.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .expiresIn(3600L)
                .build();
    }

    @Test
    @DisplayName("Should register user and return auth response")
    void shouldRegisterUserAndReturnAuthResponse() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // 什么时候
        // When
        AuthResponse result = authFacade.registerByEmail(request);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getAccessToken()).isEqualTo("test-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("test-refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should pass correct command to service during registration")
    void shouldPassCorrectCommandToServiceDuringRegistration() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(any())).thenReturn(testTokenPair);

        // 什么时候
        // When
        authFacade.registerByEmail(request);

        // 然后
        // Then
        verify(authService).registerByEmail(argThat(command ->
                command.email().equals(TEST_EMAIL) &&
                        command.password().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should login user and return auth response")
    void shouldLoginUserAndReturnAuthResponse() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // 什么时候
        // When
        AuthResponse result = authFacade.loginByEmail(request);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getAccessToken()).isEqualTo("test-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("test-refresh-token");
    }

    @Test
    @DisplayName("Should pass correct command to service during login")
    void shouldPassCorrectCommandToServiceDuringLogin() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(any())).thenReturn(testTokenPair);

        // 什么时候
        // When
        authFacade.loginByEmail(request);

        // 然后
        // Then
        verify(authService).loginByEmail(argThat(command ->
                command.email().equals(TEST_EMAIL) &&
                        command.password().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should generate tokens after successful registration")
    void shouldGenerateTokensAfterSuccessfulRegistration() {
        // 给定
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // 什么时候
        // When
        authFacade.registerByEmail(request);

        // 然后
        // Then
        verify(authService).generateTokenPair(testUser);
    }

    @Test
    @DisplayName("Should generate tokens after successful login")
    void shouldGenerateTokensAfterSuccessfulLogin() {
        // 给定
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // 什么时候
        // When
        authFacade.loginByEmail(request);

        // 然后
        // Then
        verify(authService).generateTokenPair(testUser);
    }
}
