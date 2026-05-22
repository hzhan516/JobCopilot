package io.jobcopilot.resumeassistant.api.user.dto;

/**
 * Token 校验结果枚举
 * Token validation result enum
 */
public enum TokenValidationResult {
    /**
     * 令牌有效 / Token is valid
     */
    VALID,

    /**
     * 令牌已过期 / Token has expired
     */
    EXPIRED,

    /**
     * 令牌无效（格式错误、签名不匹配等） / Token is invalid (malformed, bad signature, etc.)
     */
    INVALID
}
