package edu.asu.ser594.resumeassistant.domain.tracking.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.DomainException;

/**
 * 求职跟踪异常
 * Tracking exception
 * <p>
 * 状态流转非法时抛出 / Thrown when status transition is invalid
 */
public class TrackingException extends DomainException {

    public TrackingException(final String messageKey) {
        super(messageKey);
    }

    public TrackingException(final String messageKey, Object... args) {
        super(messageKey, args);
    }

    public TrackingException(final String messageKey, final Throwable cause, Object... args) {
        super(messageKey, cause, args);
    }
}
