package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.application.user.command.UpdateAvatarCommand;
import io.jobcopilot.resumeassistant.application.user.command.UpdateProfileCommand;
import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.domain.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户资料应用服务单元测试
 * User profile application service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Profile Application Service Tests")
class ProfileApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private ProfileApplicationService profileService;

    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .fullName("Alice Zhang")
                .avatarUrl("https://example.com/avatar.png")
                .phone("+1-555-0199")
                .targetPosition("Software Engineer")
                .preferredLocation("San Francisco, CA")
                .createdAt(LocalDateTime.now().minusDays(7))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should get profile successfully")
    void shouldGetProfileSuccessfully() {
        // 给定
        // Given
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfile));

        // 当
        // When
        UserProfile result = profileService.getProfile(USER_ID);

        // 那么
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getFullName()).isEqualTo("Alice Zhang");
        assertThat(result.getPhone()).isEqualTo("+1-555-0199");
        assertThat(result.getTargetPosition()).isEqualTo("Software Engineer");
        assertThat(result.getPreferredLocation()).isEqualTo("San Francisco, CA");
        verify(userProfileRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should throw when profile not found for get")
    void shouldThrowWhenProfileNotFoundForGet() {
        // 给定
        // Given
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> profileService.getProfile(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    @DisplayName("Should update profile successfully")
    void shouldUpdateProfileSuccessfully() {
        // 给定
        // Given
        UpdateProfileCommand command = UpdateProfileCommand.builder()
                .userId(USER_ID)
                .fullName("Alice Zhang Updated")
                .phone("+1-555-0200")
                .targetPosition("Senior Software Engineer")
                .preferredLocation("Remote")
                .build();

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        UserProfile result = profileService.updateProfile(command);

        // 那么
        // Then
        assertThat(result.getFullName()).isEqualTo("Alice Zhang Updated");
        assertThat(result.getPhone()).isEqualTo("+1-555-0200");
        assertThat(result.getTargetPosition()).isEqualTo("Senior Software Engineer");
        assertThat(result.getPreferredLocation()).isEqualTo("Remote");
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        verify(userProfileRepository).findByUserId(USER_ID);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should update profile with partial fields")
    void shouldUpdateProfileWithPartialFields() {
        // 给定
        // Given
        UpdateProfileCommand command = UpdateProfileCommand.builder()
                .userId(USER_ID)
                .fullName("Only Name Changed")
                .build();

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        UserProfile result = profileService.updateProfile(command);

        // 那么
        // Then
        assertThat(result.getFullName()).isEqualTo("Only Name Changed");
        assertThat(result.getPhone()).isEqualTo("+1-555-0199");
        assertThat(result.getTargetPosition()).isEqualTo("Software Engineer");
        assertThat(result.getPreferredLocation()).isEqualTo("San Francisco, CA");
    }

    @Test
    @DisplayName("Should throw when profile not found for update")
    void shouldThrowWhenProfileNotFoundForUpdate() {
        // 给定
        // Given
        UpdateProfileCommand command = UpdateProfileCommand.builder()
                .userId(USER_ID)
                .build();

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> profileService.updateProfile(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    @DisplayName("Should update avatar successfully")
    void shouldUpdateAvatarSuccessfully() {
        // 给定
        // Given
        UpdateAvatarCommand command = UpdateAvatarCommand.builder()
                .userId(USER_ID)
                .avatarUrl("https://storage.example.com/new-avatar.png")
                .build();

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 当
        // When
        UserProfile result = profileService.updateAvatar(command);

        // 那么
        // Then
        assertThat(result.getAvatarUrl()).isEqualTo("https://storage.example.com/new-avatar.png");
        verify(userProfileRepository).findByUserId(USER_ID);
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw when profile not found for avatar update")
    void shouldThrowWhenProfileNotFoundForAvatarUpdate() {
        // 给定
        // Given
        UpdateAvatarCommand command = UpdateAvatarCommand.builder()
                .userId(USER_ID)
                .avatarUrl("https://example.com/avatar.png")
                .build();

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> profileService.updateAvatar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");
    }
}
