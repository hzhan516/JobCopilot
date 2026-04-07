package edu.asu.ser594.resumeassistant.domain.shared.exception;

public class BusinessException extends DomainException {
    private final String messageKey;
    private final Object[] args;

    public BusinessException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = new Object[0];
    }

    public BusinessException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
