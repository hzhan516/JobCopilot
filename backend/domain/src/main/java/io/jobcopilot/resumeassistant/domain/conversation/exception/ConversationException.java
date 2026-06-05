package io.jobcopilot.resumeassistant.domain.conversation.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

public class ConversationException extends LocalizedException {
    public ConversationException(String messageKey) {
        super(messageKey);
    }

    public ConversationException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
