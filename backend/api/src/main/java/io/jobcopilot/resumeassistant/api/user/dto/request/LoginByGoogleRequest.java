package io.jobcopilot.resumeassistant.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Google 登录请求
 * Google login request
 */
@Builder
public record LoginByGoogleRequest(
        @NotBlank(message = "{validation.idToken.required}")
        String idToken,
        String captchaToken
) {
}
