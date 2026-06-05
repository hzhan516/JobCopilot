package io.jobcopilot.resumeassistant.domain.shared.exception;

/**
 * AI 服务暂时不可用异常
 * AI service is temporarily unavailable.
 */
public class AiServiceUnavailableException extends LocalizedException {

    public AiServiceUnavailableException() {
        super("ai.service.unavailable");
    }
}
