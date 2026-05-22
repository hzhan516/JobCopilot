package io.jobcopilot.resumeassistant.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件配置属性 / Email configuration properties
 */
@Component
@ConfigurationProperties(prefix = "app.email")
@Getter
@Setter
public class EmailProperties {

    private boolean enabled = false;
    private String from = "noreply@resume-assistant.local";
    private int codeExpirySeconds = 300;
    private int resendCooldownSeconds = 60;
    private int maxAttempts = 3;
}
