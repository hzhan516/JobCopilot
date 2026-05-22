package io.jobcopilot.resumeassistant.application.conversation.service;

import io.jobcopilot.resumeassistant.application.conversation.query.GetConversationQuery;
import io.jobcopilot.resumeassistant.application.conversation.query.ListConversationsQuery;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.repository.ConversationRepository;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.ConversationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 对话查询服务单元测试
 * Conversation query service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Query Service Tests")
class ConversationQueryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationQueryService queryService;

    @Test
    @DisplayName("Should get conversation successfully")
    void shouldGetConversationSuccessfully() {
        // 给定
        // Given
        Conversation conv = Conversation.reconstruct(
                CONVERSATION_ID, USER_ID, "Test Chat", ConversationStatus.ACTIVE,
                null, null, null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));

        // 当
        // When
        Conversation result = queryService.getConversation(
                new GetConversationQuery(CONVERSATION_ID, USER_ID, null, null));

        // 那么
        // Then
        assertThat(result.getId()).isEqualTo(CONVERSATION_ID);
        assertThat(result.getTitle()).isEqualTo("Test Chat");
    }

    @Test
    @DisplayName("Should throw when conversation not found")
    void shouldThrowWhenConversationNotFound() {
        // 给定
        // Given
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> queryService.getConversation(
                new GetConversationQuery(CONVERSATION_ID, USER_ID, null, null)))
                .isInstanceOf(io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException.class);
    }

    @Test
    @DisplayName("Should throw when user does not own conversation")
    void shouldThrowWhenUserDoesNotOwnConversation() {
        // 给定
        // Given
        UUID otherUser = UUID.randomUUID();
        Conversation conv = Conversation.reconstruct(
                CONVERSATION_ID, otherUser, "Other Chat", ConversationStatus.ACTIVE,
                null, null, null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conv));

        // 当&那么
        // When&Then
        assertThatThrownBy(() -> queryService.getConversation(
                new GetConversationQuery(CONVERSATION_ID, USER_ID, null, null)))
                .isInstanceOf(io.jobcopilot.resumeassistant.domain.conversation.exception.ConversationException.class);
    }

    @Test
    @DisplayName("Should list conversations")
    void shouldListConversations() {
        // 给定
        // Given
        List<Conversation> conversations = List.of(
                Conversation.reconstruct(UUID.randomUUID(), USER_ID, "Chat 1", ConversationStatus.ACTIVE,
                        null, null, null, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList())
        );
        when(conversationRepository.findAllByUserId(USER_ID)).thenReturn(conversations);

        // 当
        // When
        List<Conversation> result = queryService.listConversations(new ListConversationsQuery(USER_ID));

        // 那么
        // Then
        assertThat(result).hasSize(1);
    }
}
