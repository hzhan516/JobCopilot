package edu.asu.ser594.resumeassistant.api.user.service;

import edu.asu.ser594.resumeassistant.api.user.dto.TokenPair;
import edu.asu.ser594.resumeassistant.api.user.dto.TokenValidationResult;

/**
 * Token 服务接口
 * Token service interface
 * 定义 JWT 令牌生成与校验的契约
 * Defines contract for JWT token generation and validation
 */
public interface TokenService {
    /**
     * 为用户生成访问令牌和刷新令牌对
     * Generate access and refresh token pair for user
     *
     * @param userId 用户标识符
     * @param userId user identifier
     * @return 包含访问令牌和刷新令牌的令牌对
     * @return token pair containing access and refresh tokens
     */
    TokenPair generateTokenPair(String userId);

    /**
     * 从令牌中提取用户 ID
     * Extract user ID from token
     *
     * @param token JWT 令牌
     * @param token JWT token
     * @return 用户标识符
     * @return user identifier
     */
    String getUserIdFromToken(String token);

    /**
     * 校验令牌签名和过期时间
     * Validate token signature and expiration
     *
     * @param token JWT 令牌
     * @param token JWT token
     * @return 如果令牌有效则返回 true
     * @return true if token is valid
     */
    boolean validateToken(String token);

    /**
     * 详细校验令牌，区分过期与无效
     * Validate token with detailed result, distinguishing expired from invalid
     *
     * @param token JWT 令牌 / JWT token
     * @return 校验结果 / Validation result
     */
    TokenValidationResult validateTokenDetailed(String token);
}
