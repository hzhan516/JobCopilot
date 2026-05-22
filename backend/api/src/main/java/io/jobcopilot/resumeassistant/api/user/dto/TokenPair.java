package io.jobcopilot.resumeassistant.api.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Token 对 DTO
 * Token pair DTO
 * 包含访问令牌、刷新令牌及过期信息
 * Contains access token, refresh token and expiration info
 */
@Getter
@Builder
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;
}
