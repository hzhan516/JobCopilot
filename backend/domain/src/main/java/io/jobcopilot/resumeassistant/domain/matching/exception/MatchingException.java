package io.jobcopilot.resumeassistant.domain.matching.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

public class MatchingException extends LocalizedException {
    public MatchingException(String messageKey) {
        super(messageKey);
    }

    public MatchingException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
