package edu.asu.ser594.resumeassistant.api.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户资料响应
 * User profile response
 *
 * @param userId            用户ID / User ID
 * @param fullName          全名 / Full name
 * @param avatarUrl         头像URL / Avatar URL
 * @param phone             手机号 / Phone number
 * @param targetPosition    目标职位 / Target position
 * @param preferredLocation 期望地点 / Preferred location
 * @param createdAt         创建时间 / Created at
 * @param updatedAt         更新时间 / Updated at
 */
public record ProfileResponse(
        UUID userId,
        String fullName,
        String avatarUrl,
        String phone,
        String targetPosition,
        String preferredLocation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
