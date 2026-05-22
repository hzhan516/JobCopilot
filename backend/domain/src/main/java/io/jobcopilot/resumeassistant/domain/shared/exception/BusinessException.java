package io.jobcopilot.resumeassistant.domain.shared.exception;

public class BusinessException extends DomainException {

    public BusinessException(String messageKey) {
        super(messageKey);
    }

    public BusinessException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
