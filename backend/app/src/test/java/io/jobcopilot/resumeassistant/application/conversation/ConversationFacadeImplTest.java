package io.jobcopilot.resumeassistant.application.conversation;

import io.jobcopilot.resumeassistant.api.conversation.dto.ConversationResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.CreateConversationRequest;
import io.jobcopilot.resumeassistant.api.conversation.dto.SendMessageRequest;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationApplicationService;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationQueryService;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 对话门面实现单元测试
 * Conversation facade implementation unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Facade Implementation Tests")
class ConversationFacadeImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CONVERSATION_ID = UUID.randomUUID().toString();

    @Mock
    private ConversationApplicationService applicationService;

    @Mock
    private ConversationQueryService queryService;

    @InjectMocks
    private ConversationFacadeImpl conversationFacade;

    @Test
    @DisplayName("Should create conversation")
    void shouldCreateConversation() {
        // 给定
        // Given
        CreateConversationRequest request = new CreateConversationRequest("New Chat", null, null);
        Conversation conv = Conversation.create(USER_ID, "New Chat", null, null);
        when(applicationService.createConversation(any())).thenReturn(conv);

        // 当
        // When
        ConversationResponse response = conversationFacade.createConversation(request, USER_ID);

        // 那么
        // Then
        assertThat(response.title()).isEqualTo("New Chat");
    }

    @Test
    @DisplayName("Should send message and return conversation response")
    void shouldSendMessageAndReturnResponse() {
        // 给定
        // Given
        SendMessageRequest request = new SendMessageRequest("Hello", null);
        Conversation conv = Conversation.create(USER_ID, "Chat", null, null);
        when(applicationService.sendMessage(any())).thenReturn(conv);

        // 当
        // When
        ConversationResponse response = conversationFacade.sendMessage(CONVERSATION_ID, request, USER_ID);

        // 那么
        // Then
        assertThat(response.title()).isEqualTo("Chat");
    }

    @Test
    @DisplayName("Should get conversation")
    void shouldGetConversation() {
        // 给定
        // Given
        Conversation conv = Conversation.create(USER_ID, "Chat", null, null);
        when(queryService.getConversation(any())).thenReturn(conv);

        // 当
        // When
        ConversationResponse response = conversationFacade.getConversation(CONVERSATION_ID, USER_ID, null, null);

        // 那么
        // Then
        assertThat(response.title()).isEqualTo("Chat");
    }

    @Test
    @DisplayName("Should list conversations")
    void shouldListConversations() {
        // 给定
        // Given
        List<Conversation> conversations = List.of(
                Conversation.create(USER_ID, "Chat 1", null, null)
        );
        when(queryService.listConversations(any())).thenReturn(conversations);

        // 当
        // When
        List<ConversationResponse> result = conversationFacade.listConversations(USER_ID);

        // 那么
        // Then
        assertThat(result).hasSize(1);
    }
}
