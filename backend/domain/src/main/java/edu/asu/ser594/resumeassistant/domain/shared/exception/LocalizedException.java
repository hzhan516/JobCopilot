package edu.asu.ser594.resumeassistant.domain.shared.exception;

import lombok.Getter;

// Exception with i18n support
@Getter
public class LocalizedException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public LocalizedException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = new Object[0];
    }

    public LocalizedException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }
}
