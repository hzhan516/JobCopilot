package io.jobcopilot.resumeassistant.api.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 滑动验证码挑战响应 / Slider CAPTCHA challenge response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaChallengeResponse {

    private String captchaId;
    private int targetX;
}
