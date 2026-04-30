package edu.asu.ser594.resumeassistant.infrastructure.i18n;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.domain.shared.service.MessageProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Configures the static MessageProvider reference in ApiResponse for i18n support.
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
