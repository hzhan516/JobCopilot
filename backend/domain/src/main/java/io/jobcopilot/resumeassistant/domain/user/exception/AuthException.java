package io.jobcopilot.resumeassistant.domain.user.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.DomainException;
import lombok.Getter;

/**
 * Typed authentication exception that allows the trigger layer to map errors to precise HTTP responses.
 * 带类型的认证异常，使触发层能够将错误映射为精确的 HTTP 响应。
 */
@Getter
public class AuthException extends DomainException {

    private final ErrorType errorType;

    public AuthException(ErrorType errorType) {
        super(errorType.name());
        this.errorType = errorType;
    }

    public enum ErrorType {
        EMAIL_EXISTS,
        EMAIL_NOT_FOUND,
        INVALID_CREDENTIALS,
        EMAIL_NOT_VERIFIED,
        TOKEN_EXPIRED,
        TOKEN_INVALID,
        EMAIL_REGISTERED_WITH_PASSWORD,
        INVALID_VERIFICATION_CODE,
        VERIFICATION_CODE_EXPIRED,
        VERIFICATION_CODE_REQUIRED,
        VERIFICATION_COOLDOWN,
        EMAIL_ALREADY_REGISTERED,
        CAPTCHA_REQUIRED,
        CAPTCHA_INVALID,
        CAPTCHA_EXPIRED
    }
}
