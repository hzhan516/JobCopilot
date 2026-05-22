package io.jobcopilot.resumeassistant.api.user.dto.request;

/**
 * 更新用户资料请求
 * Update profile request
 *
 * @param fullName          全名(可选) / Full name (optional)
 * @param phone             手机号(可选) / Phone number (optional)
 * @param targetPosition    目标职位(可选) / Target position (optional)
 * @param preferredLocation 期望地点(可选) / Preferred location (optional)
 */
public record UpdateProfileRequest(
        String fullName,
        String phone,
        String targetPosition,
        String preferredLocation
) {
}
