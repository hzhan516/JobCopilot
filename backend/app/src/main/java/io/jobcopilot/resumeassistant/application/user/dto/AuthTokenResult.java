package io.jobcopilot.resumeassistant.application.user.dto;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Authentication result carrying both the user entity and the generated token pair.
 * Used by the application service so the facade can build the HTTP-layer AuthResult
 * (body + cookie) without leaking repository access outside the application layer.
 */
@Getter
@AllArgsConstructor
public class AuthTokenResult {
    private final User user;
    private final TokenPair tokens;
}
