package edu.asu.ser594.resumeassistant.infrastructure.i18n;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 配置 ApiResponse 中的静态 MessageProvider 引用以支持国际化
 * Configures the static MessageProvider reference in ApiResponse for i18n support.
 * 这使得 ApiResponse.success() 可以返回本地化的默认成功消息
 * This allows ApiResponse.success() to return localized default success messages.
 */
@Component
@RequiredArgsConstructor
public class ApiResponseConfigurer {

    private final MessageProvider messageProvider;

    @PostConstruct
    public void init() {
        ApiResponse.setMessageProvider(messageProvider);
    }
}
