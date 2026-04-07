package edu.asu.ser594.resumeassistant.api.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {
    private final UUID userId;
    private final String email;
    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;
}
