package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.domain.shared.service.MessageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationFailureMessageResolverTest {

    @Mock
    private MessageProvider messageProvider;

    @InjectMocks
    private ConversationFailureMessageResolver resolver;

    @Test
    @DisplayName("Should resolve rate limit failure message")
    void shouldResolveRateLimitFailureMessage() {
        when(messageProvider.getMessage("conversation.ai.failed.rate_limited"))
                .thenReturn("AI 请求暂时过于频繁，请几分钟后再试。");

        String message = resolver.resolve("RATE_LIMITED", "zh-CN");

        assertThat(message).isEqualTo("AI 请求暂时过于频繁，请几分钟后再试。");
    }

    @Test
    @DisplayName("Should fall back to generic failure message")
    void shouldFallbackToGenericFailureMessage() {
        when(messageProvider.getMessage("conversation.ai.failed.generic"))
                .thenReturn("AI response failed. Please try again later.");

        String message = resolver.resolve("UNEXPECTED", "en");

        assertThat(message).isEqualTo("AI response failed. Please try again later.");
    }
}
