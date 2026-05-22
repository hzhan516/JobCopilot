package io.jobcopilot.resumeassistant.infrastructure.i18n;

import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 消息提供者实现 / Message provider implementation
 */
@Component
@RequiredArgsConstructor
public class MessageProviderImpl implements MessageProvider {

    private final MessageSource messageSource;

    /**
     * 获取国际化消息 / Get internationalized message
     */
    @Override
    public String getMessage(String key) {
        return messageSource.getMessage(
                key,
                null,
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * 获取带参数的国际化消息 / Get internationalized message with arguments
     */
    @Override
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(
                key,
                args,
                LocaleContextHolder.getLocale()
        );
    }
}
