package io.jobcopilot.resumeassistant.domain.job.exception;

import io.jobcopilot.resumeassistant.domain.shared.exception.LocalizedException;

public class JobException extends LocalizedException {
    public JobException(String messageKey) {
        super(messageKey);
    }

    public JobException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
