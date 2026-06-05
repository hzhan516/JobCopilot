package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Resolves user-facing conversation failure messages from stable AI error codes.
 * 将稳定的 AI 错误码解析为用户可见的对话失败文案。
 */
@Service
@RequiredArgsConstructor
public class ConversationFailureMessageResolver {

    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String UPSTREAM_TIMEOUT = "UPSTREAM_TIMEOUT";
    public static final String UPSTREAM_UNAVAILABLE = "UPSTREAM_UNAVAILABLE";
    public static final String INVALID_MODEL_RESPONSE = "INVALID_MODEL_RESPONSE";

    private final MessageProvider messageProvider;

    public String resolve(String errorCode, String localeTag) {
        Locale previous = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(parseLocale(localeTag));
            return messageProvider.getMessage(toMessageKey(errorCode));
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
    }

    private Locale parseLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(localeTag);
    }

    private String toMessageKey(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "conversation.ai.failed.generic";
        }
        return switch (errorCode) {
            case RATE_LIMITED -> "conversation.ai.failed.rate_limited";
            case UPSTREAM_TIMEOUT -> "conversation.ai.failed.timeout";
            case UPSTREAM_UNAVAILABLE -> "conversation.ai.failed.unavailable";
            case INVALID_MODEL_RESPONSE -> "conversation.ai.failed.invalid_response";
            default -> "conversation.ai.failed.generic";
        };
    }
}
