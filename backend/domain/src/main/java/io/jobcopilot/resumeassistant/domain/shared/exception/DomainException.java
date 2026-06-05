package io.jobcopilot.resumeassistant.domain.shared.exception;

public abstract class DomainException extends LocalizedException {

    public DomainException(String messageKey) {
        super(messageKey);
    }

    public DomainException(String messageKey, Object... args) {
        super(messageKey, args);
    }

    public DomainException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause, args);
    }
}