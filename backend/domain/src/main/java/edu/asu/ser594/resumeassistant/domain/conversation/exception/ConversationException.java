package edu.asu.ser594.resumeassistant.domain.conversation.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.LocalizedException;

public class ConversationException extends LocalizedException {
    public ConversationException(String messageKey) {
        super(messageKey);
    }

    public ConversationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
