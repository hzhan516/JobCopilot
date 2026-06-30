package io.jobcopilot.resumeassistant.api.user.service;

import io.jobcopilot.resumeassistant.api.user.dto.TokenPair;
import io.jobcopilot.resumeassistant.api.user.dto.TokenValidationResult;

/**
 * Contract for JWT lifecycle management, isolating token implementation details from the security layer.
 * JWT 生命周期管理契约，将令牌实现细节与安全层解耦，支持独立替换签发策略或密钥存储方式
 */
public interface TokenService {

    /**
     * Generates a short-lived access token together with a long-lived refresh token.
     * 生成短期访问令牌与长期刷新令牌，实现无状态认证下的安全续期机制
     *
     * @param userId the subject identifier to embed in the token / 需要嵌入令牌的主体标识
     * @return access and refresh token pair / 访问令牌与刷新令牌对
     */
    TokenPair generateTokenPair(String userId);

    /**
     * Generates a token pair with a role claim embedded for RBAC authorization.
     * 生成含角色声明的令牌对，用于 RBAC 鉴权
     *
     * @param userId the subject identifier / 主体标识
     * @param role   the user role to embed / 需要嵌入的角色
     * @return access and refresh token pair / 访问令牌与刷新令牌对
     */
    TokenPair generateTokenPair(String userId, String role);

    /**
     * Parses the subject claim without validating signature or expiration; useful only after prior validation.
     * 解析主题声明而不校验签名或过期时间；仅应在已通过前置校验后使用，避免重复计算
     *
     * @param token the JWT string / JWT 字符串
     * @return the embedded user identifier / 嵌入的用户标识
     */
    String getUserIdFromToken(String token);

    /**
     * Performs a quick validity check combining signature verification and expiration inspection.
     * 快速校验签名与过期时间，适合网关或过滤器中的高频轻量检查
     *
     * @param token the JWT string / JWT 字符串
     * @return true if both signature and expiration are valid / 签名与过期时间均有效时返回 true
     */
    boolean validateToken(String token);

    /**
     * Distinguishes between an expired token and a fundamentally invalid one so the caller can decide
     * whether to trigger a refresh flow or reject the request outright.
     * 区分令牌过期与根本无效，使调用方能够决定是触发续期流程还是直接拒绝请求
     *
     * @param token the JWT string / JWT 字符串
     * @return detailed validation result / 详细校验结果
     */
    TokenValidationResult validateTokenDetailed(String token);

    /**
     * Parses the role claim from a validated token for authorization decisions.
     * 从已验证的令牌中解析角色声明，用于鉴权决策
     *
     * @param token the JWT string / JWT 字符串
     * @return the embedded role / 嵌入的角色
     */
    String getRoleFromToken(String token);
}
