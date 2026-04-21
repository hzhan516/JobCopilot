package edu.asu.ser594.resumeassistant.application.user.service;

import edu.asu.ser594.resumeassistant.application.user.command.LoginByEmailCommand;
import edu.asu.ser594.resumeassistant.application.user.command.RegisterByEmailCommand;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.service.TokenService;
import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserCredentialRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserProfileRepository;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserRepository;
import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
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
 * AuthApplicationService Unit Tests
 * 
 * Tests the application service following DDD Application Layer patterns:
 * - Use case orchestration
 * - Transaction boundary
 * - Coordination between domain objects
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

    // ==================== Register Tests ====================

    @Test
    @DisplayName("Should successfully register new user with email")
    void shouldSuccessfullyRegisterNewUserWithEmail() {
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_HASHED_PASSWORD);

        // When
        User result = authService.registerByEmail(command);

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
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

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
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(anyString())).thenReturn(TEST_HASHED_PASSWORD);

        // When
        authService.registerByEmail(command);

        // Then
        verify(userProfileRepository).save(argThat(profile ->
                profile.getUserId().equals(testUser.getId())));
    }

    @Test
    @DisplayName("Should create user credential during registration")
    void shouldCreateUserCredentialDuringRegistration() {
        // Given
        RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(userCredentialRepository.save(any(UserCredential.class))).thenReturn(testCredential);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_HASHED_PASSWORD);

        // When
        authService.registerByEmail(command);

        // Then
        verify(userCredentialRepository).save(argThat(credential ->
                credential.getCredentialType() == CredentialType.PASSWORD &&
                        credential.getCredentialValue().equals(TEST_HASHED_PASSWORD)));
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void shouldSuccessfullyLoginWithValidCredentials() {
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.of(testCredential));
        when(passwordEncoder.matches(TEST_PASSWORD, TEST_HASHED_PASSWORD)).thenReturn(true);

        // When
        User result = authService.loginByEmail(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when login with non-existent email")
    void shouldThrowExceptionWhenLoginWithNonExistentEmail() {
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email("nonexistent@example.com")
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Should throw exception when login without password credential")
    void shouldThrowExceptionWhenLoginWithoutPasswordCredential() {
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Should throw exception when login with wrong password")
    void shouldThrowExceptionWhenLoginWithWrongPassword() {
        // Given
        LoginByEmailCommand command = LoginByEmailCommand.builder()
                .email(TEST_EMAIL)
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(userCredentialRepository.findByUserIdAndType(testUser.getId(), CredentialType.PASSWORD))
                .thenReturn(Optional.of(testCredential));
        when(passwordEncoder.matches("wrongPassword", TEST_HASHED_PASSWORD)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.loginByEmail(command))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("INVALID_CREDENTIALS");
    }

    // ==================== Token Tests ====================

    @Test
    @DisplayName("Should generate token pair for user")
    void shouldGenerateTokenPairForUser() {
        // Given
        TokenPair expectedTokenPair = TokenPair.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .build();

        when(tokenService.generateTokenPair(testUser.getId().toString())).thenReturn(expectedTokenPair);

        // When
        TokenPair result = authService.generateTokenPair(testUser);

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
        // Given
        when(tokenService.generateTokenPair(anyString())).thenReturn(TokenPair.builder().build());

        // When
        authService.generateTokenPair(testUser);

        // Then
        verify(tokenService).generateTokenPair(testUser.getId().toString());
    }
}
