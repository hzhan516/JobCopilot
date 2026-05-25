package io.jobcopilot.resumeassistant.api.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HTTP-layer authentication result that separates the response body (AuthResponse)
 * from the refresh token, which must be delivered via HttpOnly cookie instead of
 * JSON to prevent XSS theft.
 */
@Getter
@AllArgsConstructor
public class AuthResult {
    private final AuthResponse authResponse;
    private final String refreshToken;
}
