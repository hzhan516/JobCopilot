package io.jobcopilot.resumeassistant.infrastructure.mail;

import io.jobcopilot.resumeassistant.domain.user.port.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 无操作邮件发送适配器 / No-op email sender adapter
 * <p>
 * 当邮件验证功能关闭时作为 fallback 实现，避免 Spring 注入失败。
 * Fallback implementation when email verification is disabled to prevent Spring injection failure.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmailSenderAdapter implements EmailSenderPort {

    @Override
    public void sendVerificationCode(String to, String code) {
        log.debug("Email verification is disabled. Skipping email send to / 邮件验证已关闭，跳过发送: {}", to);
    }
}
