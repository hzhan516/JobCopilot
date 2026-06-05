package io.jobcopilot.resumeassistant.domain.user.port;

/**
 * 邮件发送端口 / Email sender port
 * <p>
 * 领域层定义的邮件发送抽象，由基础设施层实现。
 * Domain-level abstraction for email sending, implemented by the infrastructure layer.
 */
public interface EmailSenderPort {

    /**
     * 发送验证码邮件 / Send verification code email
     *
     * @param to   收件人邮箱 / Recipient email
     * @param code 验证码 / Verification code
     */
    void sendVerificationCode(String to, String code);
}
