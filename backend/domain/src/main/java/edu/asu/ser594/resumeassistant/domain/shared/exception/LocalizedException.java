package edu.asu.ser594.resumeassistant.domain.shared.exception;

import lombok.Getter;

/**
 * Base exception that carries an i18n message key instead of a rendered message,
 * keeping the domain layer independent of presentation-layer localization infrastructure.
 * 携带国际化消息键而非渲染后文本的异常基类，使领域层独立于展示层的本地化基础设施。
 */
@Getter
public abstract class LocalizedException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public LocalizedException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = new Object[0];
    }

    public LocalizedException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public LocalizedException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = args;
    }
}
