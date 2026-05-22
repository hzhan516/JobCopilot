package io.jobcopilot.resumeassistant.domain.resume.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

public class ResumeException extends LocalizedException {
    public ResumeException(String messageKey) {
        super(messageKey);
    }

    public ResumeException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
