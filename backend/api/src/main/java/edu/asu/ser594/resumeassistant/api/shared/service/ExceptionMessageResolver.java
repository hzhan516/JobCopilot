package edu.asu.ser594.resumeassistant.api.shared.service;

import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;

/**
 * Exception message resolver interface
 * Resolves domain exception types to localized messages
 */
public interface ExceptionMessageResolver {
    /**
     * Resolve authentication error type to localized message
     *
     * @param errorType authentication error type
     * @return localized error message
     */
    String resolve(AuthException.ErrorType errorType);
}
