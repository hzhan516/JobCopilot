package edu.asu.ser594.resumeassistant.domain.job.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.LocalizedException;

public class JobException extends LocalizedException {
    public JobException(String messageKey) {
        super(messageKey);
    }

    public JobException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
