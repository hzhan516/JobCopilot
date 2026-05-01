package edu.asu.ser594.resumeassistant.application.user.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 更新头像命令
 * Update avatar command
 *
 * @param userId    用户ID / User ID
 * @param avatarUrl 头像URL / Avatar URL
 */
@Builder
public record UpdateAvatarCommand(
        UUID userId,
        String avatarUrl
) {
}
