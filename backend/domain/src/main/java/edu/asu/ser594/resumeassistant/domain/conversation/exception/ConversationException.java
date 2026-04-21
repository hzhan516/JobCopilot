package edu.asu.ser594.resumeassistant.domain.conversation.exception;

public class ConversationException extends RuntimeException {
    
    public enum ErrorType {
        NOT_FOUND,
        ACCESS_DENIED,
        CLOSED_CONVERSATION,
        INVALID_RESUME_VERSION
    }

    private final ErrorType errorType;

    public ConversationException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
