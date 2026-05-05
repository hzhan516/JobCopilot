package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.ProfileResponse;
import edu.asu.ser594.resumeassistant.application.user.command.UpdateAvatarCommand;
import edu.asu.ser594.resumeassistant.application.user.command.UpdateProfileCommand;
import edu.asu.ser594.resumeassistant.application.user.service.ProfileApplicationService;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户资料门面实现单元测试
 * User profile facade implementation unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Profile Facade Implementation Tests")
class ProfileFacadeImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProfileApplicationService profileApplicationService;

    @InjectMocks
    private ProfileFacadeImpl profileFacade;

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
    @DisplayName("Should get profile and return response")
    void shouldGetProfileAndReturnResponse() {
        // 给定
        // Given
        when(profileApplicationService.getProfile(USER_ID)).thenReturn(testProfile);

        // 当
        // When
        ProfileResponse response = profileFacade.getProfile(USER_ID);

        // 那么
        // Then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.fullName()).isEqualTo("Alice Zhang");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.phone()).isEqualTo("+1-555-0199");
        assertThat(response.targetPosition()).isEqualTo("Software Engineer");
        assertThat(response.preferredLocation()).isEqualTo("San Francisco, CA");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update profile and return response")
    void shouldUpdateProfileAndReturnResponse() {
        // 给定
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Alice Zhang Updated",
                "+1-555-0200",
                "Senior Software Engineer",
                "Remote"
        );

        when(profileApplicationService.updateProfile(any(UpdateProfileCommand.class)))
                .thenAnswer(inv -> {
                    UpdateProfileCommand cmd = inv.getArgument(0);
                    testProfile = testProfile.updateProfile(cmd.fullName(), cmd.phone(), cmd.targetPosition(), cmd.preferredLocation());
                    return testProfile;
                });

        // 当
        // When
        ProfileResponse response = profileFacade.updateProfile(USER_ID, request);

        // 那么
        // Then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.fullName()).isEqualTo("Alice Zhang Updated");
        assertThat(response.phone()).isEqualTo("+1-555-0200");
        assertThat(response.targetPosition()).isEqualTo("Senior Software Engineer");
        assertThat(response.preferredLocation()).isEqualTo("Remote");
        verify(profileApplicationService).updateProfile(argThat(command ->
                command.userId().equals(USER_ID) &&
                        "Alice Zhang Updated".equals(command.fullName()) &&
                        "+1-555-0200".equals(command.phone()) &&
                        "Senior Software Engineer".equals(command.targetPosition()) &&
                        "Remote".equals(command.preferredLocation())));
    }

    @Test
    @DisplayName("Should update avatar and return response")
    void shouldUpdateAvatarAndReturnResponse() {
        // 给定
        // Given
        UpdateAvatarRequest request = new UpdateAvatarRequest("https://storage.example.com/new-avatar.png");

        when(profileApplicationService.updateAvatar(any(UpdateAvatarCommand.class)))
                .thenAnswer(inv -> {
                    UpdateAvatarCommand cmd = inv.getArgument(0);
                    testProfile = testProfile.updateAvatar(cmd.avatarUrl());
                    return testProfile;
                });

        // 当
        // When
        ProfileResponse response = profileFacade.updateAvatar(USER_ID, request);

        // 那么
        // Then
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.avatarUrl()).isEqualTo("https://storage.example.com/new-avatar.png");
        verify(profileApplicationService).updateAvatar(argThat(command ->
                command.userId().equals(USER_ID) &&
                        "https://storage.example.com/new-avatar.png".equals(command.avatarUrl())));
    }
}
