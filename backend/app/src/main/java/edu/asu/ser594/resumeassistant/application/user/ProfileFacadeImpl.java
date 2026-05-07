package edu.asu.ser594.resumeassistant.application.user;

import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.ProfileResponse;
import edu.asu.ser594.resumeassistant.api.user.facade.ProfileFacade;
import edu.asu.ser594.resumeassistant.application.user.command.UpdateAvatarCommand;
import edu.asu.ser594.resumeassistant.application.user.command.UpdateProfileCommand;
import edu.asu.ser594.resumeassistant.application.user.service.ProfileApplicationService;
import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 用户资料门面实现
 * User profile facade implementation
 */
@Component
@RequiredArgsConstructor
public class ProfileFacadeImpl implements ProfileFacade {

    private final ProfileApplicationService profileApplicationService;

    @Override
    public ProfileResponse getProfile(UUID userId) {
        UserProfile profile = profileApplicationService.getProfile(userId);
        return mapToResponse(profile);
    }

    @Override
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UpdateProfileCommand command = UpdateProfileCommand.builder()
                .userId(userId)
                .fullName(request.fullName())
                .phone(request.phone())
                .targetPosition(request.targetPosition())
                .preferredLocation(request.preferredLocation())
                .build();
        UserProfile profile = profileApplicationService.updateProfile(command);
        return mapToResponse(profile);
    }

    @Override
    public ProfileResponse updateAvatar(UUID userId, UpdateAvatarRequest request) {
        UpdateAvatarCommand command = UpdateAvatarCommand.builder()
                .userId(userId)
                .avatarUrl(request.avatarUrl())
                .build();
        UserProfile profile = profileApplicationService.updateAvatar(command);
        return mapToResponse(profile);
    }

    /**
     * 将领域实体映射为响应DTO
     * Map domain entity to response DTO
     */
    private ProfileResponse mapToResponse(UserProfile profile) {
        return new ProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.getPhone(),
                profile.getTargetPosition(),
                profile.getPreferredLocation(),
                profile.getCreatedAt().atOffset(ZoneOffset.UTC),
                profile.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
