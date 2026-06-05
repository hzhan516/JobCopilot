package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.service.TokenService;
import io.jobcopilot.resumeassistant.application.user.command.LoginByEmailCommand;
import io.jobcopilot.resumeassistant.application.user.command.RegisterByEmailCommand;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.entity.UserCredential;
import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.domain.user.exception.AuthException;
import io.jobcopilot.resumeassistant.app.config.EmailProperties;
import io.jobcopilot.resumeassistant.domain.user.port.GoogleTokenVerifierPort;
import io.jobcopilot.resumeassistant.domain.user.repository.UserCredentialRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserOAuthBindingRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserProfileRepository;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.domain.user.service.PasswordEncoder;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthApplicationService 单元测试
 * AuthApplicationService Unit Tests
 * <p>
 * 按照 DDD 应用程序层模式测试应用程序服务：
 * Tests the application service following DDD Application Layer patterns:
 * - 用例编排
 * - Use case orchestration
 * - 交易边界
 * - Transaction boundary
 * - 域对象之间的协调
 * - Coordination between domain objects
 * - 没有领域逻辑，只有工作流程
 * - No domain logic, only workflow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Application Service Tests")
class AuthApplicationServiceTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_HASHED_PASSWORD = "hashedPassword123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserOAuthBindingRepository userOAuthBindingRepository;

    @Mock
    private GoogleTokenVerifierPort googleTokenVerifierPort;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private AuthApplicationService authService;

    private User testUser;
    private UserProfile testProfile;
    private UserCredential testCredential;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .emailVerified(false)
                .role(UserRole.JOB_SEEKER)
                .status(UserStatus.ACTIVE)
                .authProvider(OAuthProvider.EMAIL)
                .version(0L)
                .build();

        testProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .build();

        testCredential = UserCredential.builder()
                .id(UUID.randomUUID())
                .userId(testUser.getId())
                .credentialType(CredentialType.PASSWORD)
                .credentialValue(TEST_HASHED_PASSWORD)
                .build();
    }

    // ==================== 注册测试 ====================
    // ==================== Register Tests ====================

    @Test
    @DisplayName("Should successfully register new user with email")
    void shouldSuccessfullyRegisterNewUserWithEmail() {
        // 给定
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(emailProperties.isEnabled()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_HASHED_PASSWORD);

        // 什么时候
        // When
        User result = authService.registerByEmail(command);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(userCredentialRepository).save(any(UserCredential.class));
        verify(passwordEncoder).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // 给定
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // 什么时候&然后
        // When & Then
        assertThatThrownBy(() -> authService.registerByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("EMAIL_EXISTS");

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create user profile during registration")
    void shouldCreateUserProfileDuringRegistration() {
        // 给定
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(emailProperties.isEnabled()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(anyString())).thenReturn(TEST_HASHED_PASSWORD);

        // 什么时候
        // When
        authService.registerByEmail(command);

        // 然后
        // Then
        verify(userProfileRepository).save(argThat(profile ->
                profile.getUserId().equals(testUser.getId())));
    }

    @Test
    @DisplayName("Should create user credential during registration")
    void shouldCreateUserCredentialDuringRegistration() {
        // 给定
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(emailProperties.isEnabled()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_HASHED_PASSWORD);

        // 什么时候
        // When
        authService.registerByEmail(command);

        // 然后
        // Then
        verify(userCredentialRepository).save(argThat(credential ->
                credential.getCredentialType() == CredentialType.PASSWORD &&
                        credential.getCredentialValue().equals(TEST_HASHED_PASSWORD)));
    }

    // ==================== 登录测试 ====================
    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void shouldSuccessfullyLoginWithValidCredentials() {
        // 给定
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.of(testCredential));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_HASHED_PASSWORD)).thenReturn(true);

        // 什么时候
        // When
        User result = authService.loginByEmail(command);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when login with non-existent email")
    void shouldThrowExceptionWhenLoginWithNonExistentEmail() {
        // 给定
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email("nonexistent@example.com")
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // 什么时候&然后
        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Should throw exception when login without password credential")
    void shouldThrowExceptionWhenLoginWithoutPasswordCredential() {
        // 给定
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.empty());

        // 什么时候&然后
        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Should throw exception when login with wrong password")
    void shouldThrowExceptionWhenLoginWithWrongPassword() {
        // 给定
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.of(testCredential));
        when(passwordEncoder.matches("wrongPassword", TEST_HASHED_PASSWORD)).thenReturn(false);

        // 什么时候&然后
        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    // ==================== 令牌测试 ====================
    // ==================== Token Tests ====================

    @Test
    @DisplayName("Should generate token pair for user")
    void shouldGenerateTokenPairForUser() {
        // 给定
        // Given
        TokenPair expectedTokenPair = TokenPair.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();

        when(tokenService.generateTokenPair(testUser.getId().toString())).thenReturn(expectedTokenPair);

        // 什么时候
        // When
        TokenPair result = authService.generateTokenPair(testUser);

        // 然后
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
        verify(tokenService).generateTokenPair(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should call token service with correct user ID")
    void shouldCallTokenServiceWithCorrectUserId() {
        // 给定
        // Given
        when(tokenService.generateTokenPair(anyString())).thenReturn(TokenPair.builder().build());

        // 什么时候
        // When
        authService.generateTokenPair(testUser);

        // 然后
        // Then
        verify(tokenService).generateTokenPair(testUser.getId().toString());
    }
}
