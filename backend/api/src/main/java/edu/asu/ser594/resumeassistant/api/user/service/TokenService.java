package edu.asu.ser594.resumeassistant.api.user.service;

import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;

/**
 * Token service interface
 * Defines contract for JWT token generation and validation
 */
public interface TokenService {
    /**
     * Generate access and refresh token pair for user
     *
     * @param userId user identifier
     * @return token pair containing access and refresh tokens
     */
    TokenPair generateTokenPair(String userId);

    /**
     * Extract user ID from token
     *
     * @param token JWT token
     * @return user identifier
     */
    String getUserIdFromToken(String token);

    /**
     * Validate token signature and expiration
     *
     * @param token JWT token
     * @return true if token is valid
     */
    boolean validateToken(String token);
}
