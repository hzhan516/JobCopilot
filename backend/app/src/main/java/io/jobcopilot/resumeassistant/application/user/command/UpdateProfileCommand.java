package io.jobcopilot.resumeassistant.application.user.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 更新用户资料命令
 * Update profile command
 *
 * @param userId            用户ID / User ID
 * @param fullName          全名 / Full name
 * @param phone             手机号 / Phone number
 * @param targetPosition    目标职位 / Target position
 * @param preferredLocation 期望地点 / Preferred location
 */
@Builder
public record UpdateProfileCommand(
        UUID userId,
        String fullName,
        String phone,
        String targetPosition,
        String preferredLocation
) {
}
