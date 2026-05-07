// 认证异常 / AuthException
package edu.asu.ser594.resumeassistant.domain.user.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.DomainException;
import lombok.Getter;

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
        EMAIL_REGISTERED_WITH_PASSWORD
    }
}