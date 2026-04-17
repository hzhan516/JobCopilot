package edu.asu.ser594.resumeassistant.domain.tracking.exception;

import edu.asu.ser594.resumeassistant.domain.shared.exception.DomainException;

/**
 * 求职跟踪异常
 * Tracking exception
 *
 * 状态流转非法时抛出 / Thrown when status transition is invalid
 */
public class TrackingException extends DomainException {

    public TrackingException(final String message) {
        super(message);
    }

    public TrackingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
