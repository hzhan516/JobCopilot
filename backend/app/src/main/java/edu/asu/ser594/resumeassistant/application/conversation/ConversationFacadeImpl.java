package edu.asu.ser594.resumeassistant.application.conversation;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.MessageResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.application.conversation.service.ConversationApplicationService;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对话外观实现类
 * Conversation facade implementation
 */
@Component
@RequiredArgsConstructor
public class ConversationFacadeImpl implements ConversationFacade {

    private final ConversationApplicationService applicationService;

    @Override
    public ApiResponse<ConversationResponse> createConversation(CreateConversationRequest request, String userId) {
        CreateConversationCommand command = CreateConversationCommand.builder()
            .userId(userId)
            .title(request.title())
            .resumeVersionId(request.resumeVersionId())
            .build();
            
        Conversation conversation = applicationService.createConversation(command);
        return ApiResponse.success(mapToResponse(conversation));
    }

    @Override
    public ApiResponse<ConversationResponse> sendMessage(String conversationId, SendMessageRequest request, String userId) {
        SendMessageCommand command = SendMessageCommand.builder()
            .conversationId(conversationId)
            .userId(userId)
            .role(MessageRole.USER) // Assuming request comes from user
            .content(request.content())
            .build();
            
        Conversation conversation = applicationService.sendMessage(command);
        return ApiResponse.success(mapToResponse(conversation));
    }

    @Override
    public ApiResponse<ConversationResponse> getConversation(String conversationId, String userId) {
        Conversation conversation = applicationService.getConversation(conversationId, userId);
        return ApiResponse.success(mapToResponse(conversation));
    }

    @Override
    public ApiResponse<List<ConversationResponse>> listConversations(String userId) {
        List<ConversationResponse> responses = applicationService.listConversations(userId).stream()
            .map(this::mapToResponse)
            .toList();
        return ApiResponse.success(responses);
    }

    @Override
    public ApiResponse<Void> closeConversation(String conversationId, String userId) {
        applicationService.closeConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> deleteConversation(String conversationId, String userId) {
        applicationService.deleteConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 映射领域聚合根到响应 DTO
     * Map domain aggregate root to response DTO
     */
    private ConversationResponse mapToResponse(Conversation conversation) {
        List<MessageResponse> messageResponses = conversation.getMessages().stream()
            .map(this::mapMessageToResponse)
            .toList();

        return new ConversationResponse(
            conversation.getId(),
            conversation.getUserId(),
            conversation.getTitle(),
            conversation.getStatus().name(),
            conversation.getResumeVersionId(),
            messageResponses,
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
    }

    /**
     * 映射消息实体到响应 DTO
     * Map message entity to response DTO
     */
    private MessageResponse mapMessageToResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getRole().name(),
            message.getContent(),
            message.getSequence(),
            message.getCreatedAt()
        );
    }
}
