package edu.asu.ser594.resumeassistant.api.shared.service;

import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;

/**
 * Bridges domain-specific auth errors to human-readable localized messages without leaking internal error codes to clients.
 * 将领域认证错误映射为本地化可读消息，避免将内部错误码直接暴露给客户端
 */
public interface ExceptionMessageResolver {

    /**
     * Resolves the given auth error type into a message suitable for API responses.
     * 将指定认证错误类型解析为适用于 API 响应的消息文本
     *
     * @param errorType the domain error classification / 领域错误分类
     * @return localized message text / 本地化消息文本
     */
    String resolve(AuthException.ErrorType errorType);
}
