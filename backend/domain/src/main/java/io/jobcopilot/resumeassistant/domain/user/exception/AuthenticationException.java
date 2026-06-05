package io.jobcopilot.resumeassistant.domain.user.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

public class AuthenticationException extends LocalizedException {
    public AuthenticationException(String messageKey) {
        super(messageKey);
    }

    public AuthenticationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
