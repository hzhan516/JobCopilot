package edu.asu.ser594.resumeassistant.api.user.dto.request;

/**
 * 更新头像请求
 * Update avatar request
 *
 * @param avatarUrl 头像URL / Avatar URL
 */
public record UpdateAvatarRequest(
        String avatarUrl
) {
}
