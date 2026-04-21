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
 * AuthFacadeImpl Unit Tests
 * 
 * Tests the facade implementation that acts as an anti-corruption layer:
 * - DTO to Command conversion
 * - Response assembly
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
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // When
        AuthResponse result = authFacade.registerByEmail(request);

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
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(any())).thenReturn(testTokenPair);

        // When
        authFacade.registerByEmail(request);

        // Then
        verify(authService).registerByEmail(argThat(command ->
                command.email().equals(TEST_EMAIL) &&
                        command.password().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should login user and return auth response")
    void shouldLoginUserAndReturnAuthResponse() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // When
        AuthResponse result = authFacade.loginByEmail(request);

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
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(any())).thenReturn(testTokenPair);

        // When
        authFacade.loginByEmail(request);

        // Then
        verify(authService).loginByEmail(argThat(command ->
                command.email().equals(TEST_EMAIL) &&
                        command.password().equals(TEST_PASSWORD)));
    }

    @Test
    @DisplayName("Should generate tokens after successful registration")
    void shouldGenerateTokensAfterSuccessfulRegistration() {
        // Given
        RegisterByEmailRequest request = RegisterByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.registerByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // When
        authFacade.registerByEmail(request);

        // Then
        verify(authService).generateTokenPair(testUser);
    }

    @Test
    @DisplayName("Should generate tokens after successful login")
    void shouldGenerateTokensAfterSuccessfulLogin() {
        // Given
        LoginByEmailRequest request = LoginByEmailRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(authService.loginByEmail(any())).thenReturn(testUser);
        when(authService.generateTokenPair(testUser)).thenReturn(testTokenPair);

        // When
        authFacade.loginByEmail(request);

        // Then
        verify(authService).generateTokenPair(testUser);
    }
}
