package edu.asu.ser594.resumeassistant.trigger.http.controller.conversation;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 对话控制器测试 / Conversation controller tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation Controller Tests")
class ConversationControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock
    private ConversationFacade conversationFacade;

    @InjectMocks
    private ConversationController conversationController;

    private ConversationResponse testConversationResponse;

    // 准备 / Given
    @BeforeEach
    void setUp() {
        testConversationResponse = new ConversationResponse(
                CONVERSATION_ID.toString(),
                USER_ID.toString(),
                "Test Conversation",
                "ACTIVE",
                null,
                null,
                Collections.emptyList(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create conversation and return response")
    void shouldCreateConversationAndReturnResponse() {
        // 准备 / Given
        CreateConversationRequest request = new CreateConversationRequest("Test", null, null);
        when(conversationFacade.createConversation(request, USER_ID)).thenReturn(testConversationResponse);

        // 执行 / When
        ApiResponse<ConversationResponse> response = conversationController.createConversation(request, USER_ID);

        // 验证 / Then
        assertThat(response.getData().title()).isEqualTo("Test Conversation");
        verify(conversationFacade).createConversation(request, USER_ID);
    }

    @Test
    @DisplayName("Should send message and return response")
    void shouldSendMessageAndReturnResponse() {
        // 准备 / Given
        SendMessageRequest request = new SendMessageRequest("Hello", List.of("https://file.pdf"));
        when(conversationFacade.sendMessage(CONVERSATION_ID.toString(), request, USER_ID))
                .thenReturn(testConversationResponse);

        // 执行 / When
        ApiResponse<ConversationResponse> response = conversationController.sendMessage(
                CONVERSATION_ID.toString(), request, USER_ID);

        // 验证 / Then
        assertThat(response.getData()).isNotNull();
        verify(conversationFacade).sendMessage(CONVERSATION_ID.toString(), request, USER_ID);
    }

    @Test
    @DisplayName("Should get conversation with pagination parameters")
    void shouldGetConversationWithPaginationParameters() {
        // 准备 / Given
        when(conversationFacade.getConversation(CONVERSATION_ID.toString(), USER_ID, 0, 10))
                .thenReturn(testConversationResponse);

        // 执行 / When
        ApiResponse<ConversationResponse> response = conversationController.getConversation(
                CONVERSATION_ID.toString(), 0, 10, USER_ID);

        // 验证 / Then
        assertThat(response.getData().conversationId()).isEqualTo(CONVERSATION_ID.toString());
        verify(conversationFacade).getConversation(CONVERSATION_ID.toString(), USER_ID, 0, 10);
    }

    @Test
    @DisplayName("Should get conversation without pagination")
    void shouldGetConversationWithoutPagination() {
        // 准备 / Given
        when(conversationFacade.getConversation(CONVERSATION_ID.toString(), USER_ID, null, null))
                .thenReturn(testConversationResponse);

        // 执行 / When
        ApiResponse<ConversationResponse> response = conversationController.getConversation(
                CONVERSATION_ID.toString(), null, null, USER_ID);

        // 验证 / Then
        assertThat(response.getData()).isNotNull();
    }

    @Test
    @DisplayName("Should list conversations")
    void shouldListConversations() {
        // 准备 / Given
        when(conversationFacade.listConversations(USER_ID))
                .thenReturn(List.of(testConversationResponse));

        // 执行 / When
        ApiResponse<List<ConversationResponse>> response = conversationController.listConversations(USER_ID);

        // 验证 / Then
        assertThat(response.getData()).hasSize(1);
    }

    @Test
    @DisplayName("Should close conversation")
    void shouldCloseConversation() {
        // 执行 / When
        ApiResponse<Void> response = conversationController.closeConversation(
                CONVERSATION_ID.toString(), USER_ID);

        // 验证 / Then
        assertThat(response.getCode()).isEqualTo(200);
        verify(conversationFacade).closeConversation(CONVERSATION_ID.toString(), USER_ID);
    }

    @Test
    @DisplayName("Should delete conversation")
    void shouldDeleteConversation() {
        // 执行 / When
        ApiResponse<Void> response = conversationController.deleteConversation(
                CONVERSATION_ID.toString(), USER_ID);

        // 验证 / Then
        assertThat(response.getCode()).isEqualTo(200);
        verify(conversationFacade).deleteConversation(CONVERSATION_ID.toString(), USER_ID);
    }

    @Test
    @DisplayName("Should upload attachment")
    void shouldUploadAttachment() {
        // 准备 / Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(conversationFacade.uploadAttachment(CONVERSATION_ID.toString(), mockFile, USER_ID))
                .thenReturn("https://minio.example.com/file.pdf");

        // 执行 / When
        ApiResponse<String> response = conversationController.uploadAttachment(
                CONVERSATION_ID.toString(), mockFile, USER_ID);

        // 验证 / Then
        assertThat(response.getData()).isEqualTo("https://minio.example.com/file.pdf");
    }
}
