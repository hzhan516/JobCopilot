package io.jobcopilot.resumeassistant.infrastructure.mail;

import io.jobcopilot.resumeassistant.domain.user.port.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * JavaMail 邮件发送适配器 / JavaMail email sender adapter
 * <p>
 * 基于 Spring Boot Mail Starter 实现 EmailSenderPort 接口。
 * 仅在 app.email.enabled=true 时激活。
 * Implementation of EmailSenderPort based on Spring Boot Mail Starter.
 * Activated only when app.email.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "true")
@ConditionalOnBean(JavaMailSender.class)
@RequiredArgsConstructor
public class JavaMailEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@resume-assistant.local}")
    private String from;

    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Resume Assistant - Verification Code / 智能求职助手 - 验证码");
            helper.setText(buildHtmlBody(code), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send verification email / 发送验证邮件失败: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildHtmlBody(String code) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #2563eb;">Resume Assistant / 智能求职助手</h2>
                <p>Your verification code is / 您的验证码是:</p>
                <div style="font-size: 32px; font-weight: bold; color: #1f2937; letter-spacing: 4px; padding: 16px; background: #f3f4f6; border-radius: 8px; text-align: center; margin: 16px 0;">
                    %s
                </div>
                <p style="color: #6b7280; font-size: 14px;">This code will expire in 5 minutes. / 此验证码5分钟内有效。</p>
                <p style="color: #6b7280; font-size: 14px;">If you did not request this, please ignore this email. / 如非本人操作，请忽略此邮件。</p>
            </div>
            """.formatted(code);
    }
}
