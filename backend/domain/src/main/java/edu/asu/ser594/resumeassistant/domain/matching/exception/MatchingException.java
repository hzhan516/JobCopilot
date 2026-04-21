package edu.asu.ser594.resumeassistant.domain.matching.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.LocalizedException;

public class MatchingException extends LocalizedException {
    public MatchingException(String messageKey) {
        super(messageKey);
    }

    public MatchingException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
