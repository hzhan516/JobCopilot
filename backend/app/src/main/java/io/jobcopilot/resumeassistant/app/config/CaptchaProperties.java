package io.jobcopilot.resumeassistant.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 滑动验证码配置属性 / Slider CAPTCHA configuration properties
 */
@Component
@ConfigurationProperties(prefix = "app.captcha")
@Getter
@Setter
public class CaptchaProperties {

    private boolean enabled = true;
    private int tolerance = 8;
    private int tokenExpirySeconds = 300;
    private int trackWidth = 300;
    private int maxAttempts = 5;
    private String redisKeyPrefix = "ra:captcha:";
}
