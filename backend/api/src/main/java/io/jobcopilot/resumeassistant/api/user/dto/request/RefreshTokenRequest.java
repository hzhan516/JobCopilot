package io.jobcopilot.resumeassistant.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌请求
 * Refresh token request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     * Refresh token
     */
    @NotBlank(message = "{validation.token.required}")
    private String refreshToken;
}
