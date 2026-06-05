package io.jobcopilot.resumeassistant.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 滑动验证码校验请求 / Slider CAPTCHA verify request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVerifyRequest {

    @NotBlank(message = "{validation.captcha.id.required}")
    private String captchaId;

    @NotNull(message = "{validation.captcha.offset.required}")
    private Integer offsetX;
}
