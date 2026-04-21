package edu.asu.ser594.resumeassistant.domain.user.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.LocalizedException;

public class AuthenticationException extends LocalizedException {
    public AuthenticationException(String messageKey) {
        super(messageKey);
    }

    public AuthenticationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
