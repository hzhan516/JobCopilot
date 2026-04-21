package edu.asu.ser594.resumeassistant.api.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Token pair DTO
 * Contains access token, refresh token and expiration info
 */
@Getter
@Builder
public class TokenPair {
    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;
}
