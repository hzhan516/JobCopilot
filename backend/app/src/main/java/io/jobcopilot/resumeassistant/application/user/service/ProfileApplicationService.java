package io.jobcopilot.resumeassistant.application.user.service;

import io.jobcopilot.resumeassistant.application.user.command.UpdateAvatarCommand;
import io.jobcopilot.resumeassistant.application.user.command.UpdateProfileCommand;
import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.domain.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 用户资料应用服务
 * User profile application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileApplicationService {

    private final UserProfileRepository userProfileRepository;

    /**
     * 获取用户资料
     * Get user profile
     *
     * @param userId 用户ID / User ID
     * @return 用户资料 / User profile
     */
    public UserProfile getProfile(UUID userId) {
        log.debug("Getting profile for user: {}", userId);
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));
    }

    /**
     * 更新用户资料
     * Update user profile
     *
     * @param command 更新命令 / Update command
     * @return 更新后的用户资料 / Updated user profile
     */
    @Transactional(timeout = 30)
    public UserProfile updateProfile(UpdateProfileCommand command) {
        log.info("Updating profile for user: {}", command.userId());
        UserProfile profile = userProfileRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + command.userId()));

        UserProfile updated = profile.updateProfile(command.fullName(), command.phone(), command.targetPosition(), command.preferredLocation());
        return userProfileRepository.save(updated);
    }

    /**
     * 更新用户头像
     * Update user avatar
     *
     * @param command 头像更新命令 / Avatar update command
     * @return 更新后的用户资料 / Updated user profile
     */
    @Transactional(timeout = 30)
    public UserProfile updateAvatar(UpdateAvatarCommand command) {
        log.info("Updating avatar for user: {}", command.userId());
        UserProfile profile = userProfileRepository.findByUserId(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + command.userId()));

        UserProfile updated = profile.updateAvatar(command.avatarUrl());
        return userProfileRepository.save(updated);
    }
}
