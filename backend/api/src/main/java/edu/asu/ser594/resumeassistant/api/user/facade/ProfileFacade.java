package edu.asu.ser594.resumeassistant.api.user.facade;

import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateAvatarRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.request.UpdateProfileRequest;
import edu.asu.ser594.resumeassistant.api.user.dto.response.ProfileResponse;

import java.util.UUID;

/**
 * 用户资料门面接口
 * User profile facade interface
 */
public interface ProfileFacade {

    /**
     * 获取当前用户资料
     * Get current user profile
     *
     * @param userId 用户ID / User ID
     * @return 用户资料响应 / Profile response
     */
    ProfileResponse getProfile(UUID userId);

    /**
     * 更新用户资料
     * Update user profile
     *
     * @param userId  用户ID / User ID
     * @param request 更新请求 / Update request
     * @return 更新后的用户资料 / Updated profile
     */
    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * 更新用户头像
     * Update user avatar
     *
     * @param userId  用户ID / User ID
     * @param request 头像更新请求 / Avatar update request
     * @return 更新后的用户资料 / Updated profile
     */
    ProfileResponse updateAvatar(UUID userId, UpdateAvatarRequest request);
}
