package edu.asu.ser594.resumeassistant.api.shared.service;

import edu.asu.ser594.resumeassistant.domain.user.exception.AuthException;

/**
 * 异常消息解析器接口
 * Exception message resolver interface
 * 将领域异常类型解析为本地化消息
 * Resolves domain exception types to localized messages
 */
public interface ExceptionMessageResolver {
    /**
     * 将认证错误类型解析为本地化消息
     * Resolve authentication error type to localized message
     *
     * @param errorType 认证错误类型
     * @param errorType authentication error type
     * @return 本地化错误消息
     * @return localized error message
     */
    String resolve(AuthException.ErrorType errorType);
}
