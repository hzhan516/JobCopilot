package io.jobcopilot.resumeassistant.trigger.http.controller.user;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import io.jobcopilot.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import io.jobcopilot.resumeassistant.api.user.dto.response.ProfileResponse;
import io.jobcopilot.resumeassistant.api.user.facade.ProfileFacade;
import io.jobcopilot.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 用户资料控制器
 * User profile controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ProfileController {

    private final ProfileFacade profileFacade;

    /**
     * 获取当前登录用户的个人资料
     * Get current logged-in user's profile
     */
    @GetMapping("/v1/profile")
    public ApiResponse<ProfileResponse> getProfile(
            @CurrentUser UUID userId) {
        log.info("User {} fetching profile", userId);
        ProfileResponse response = profileFacade.getProfile(userId);
        return ApiResponse.success(response);
    }

    /**
     * 更新个人资料
     * Update profile
     */
    @PutMapping("/v1/profile")
    public ApiResponse<ProfileResponse> updateProfile(
            @CurrentUser UUID userId,
            @Validated @RequestBody UpdateProfileRequest request) {
        log.info("User {} updating profile", userId);
        ProfileResponse response = profileFacade.updateProfile(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 更新头像 URL
     * Update avatar URL
     */
    @PutMapping("/v1/profile/avatar")
    public ApiResponse<ProfileResponse> updateAvatar(
            @CurrentUser UUID userId,
            @Validated @RequestBody UpdateAvatarRequest request) {
        log.info("User {} updating avatar", userId);
        ProfileResponse response = profileFacade.updateAvatar(userId, request);
        return ApiResponse.success(response);
    }
}
