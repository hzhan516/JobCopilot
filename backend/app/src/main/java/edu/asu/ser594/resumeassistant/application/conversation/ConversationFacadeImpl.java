package edu.asu.ser594.resumeassistant.application.conversation;

import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.MessageResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.application.conversation.command.CreateConversationCommand;
import edu.asu.ser594.resumeassistant.application.conversation.command.SendMessageCommand;
import edu.asu.ser594.resumeassistant.application.conversation.query.GetConversationQuery;
import edu.asu.ser594.resumeassistant.application.conversation.query.ListConversationsQuery;
import edu.asu.ser594.resumeassistant.application.conversation.service.ConversationApplicationService;
import edu.asu.ser594.resumeassistant.application.conversation.service.ConversationQueryService;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Conversation;
import edu.asu.ser594.resumeassistant.domain.conversation.entity.Message;
import edu.asu.ser594.resumeassistant.domain.conversation.valueobject.MessageRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 对话外观实现类
 * Conversation facade implementation
 */
@Component
@RequiredArgsConstructor
public class ConversationFacadeImpl implements ConversationFacade {

    private final ConversationApplicationService applicationService;
    private final ConversationQueryService queryService;

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request, String userId) {
        UUID resumeVersionId = request.resumeVersionId() != null && !request.resumeVersionId().isEmpty()
                ? UUID.fromString(request.resumeVersionId())
                : null;
        CreateConversationCommand command = CreateConversationCommand.builder()
            .userId(UUID.fromString(userId))
            .title(request.title())
            .resumeVersionId(resumeVersionId)
            .build();
            
        Conversation conversation = applicationService.createConversation(command);
        return mapToResponse(conversation);
    }

    @Override
    public ConversationResponse sendMessage(String conversationId, SendMessageRequest request, String userId) {
        SendMessageCommand command = SendMessageCommand.builder()
            .conversationId(UUID.fromString(conversationId))
            .userId(UUID.fromString(userId))
            .role(MessageRole.USER) // Assuming request comes from user
            .content(request.content())
            .build();
            
        Conversation conversation = applicationService.sendMessage(command);
        return mapToResponse(conversation);
    }

    @Override
    public ConversationResponse getConversation(String conversationId, String userId) {
        GetConversationQuery query = new GetConversationQuery(
            UUID.fromString(conversationId),
            UUID.fromString(userId)
        );
        Conversation conversation = queryService.getConversation(query);
        return mapToResponse(conversation);
    }

    @Override
    public List<ConversationResponse> listConversations(String userId) {
        ListConversationsQuery query = new ListConversationsQuery(UUID.fromString(userId));
        return queryService.listConversations(query).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public void closeConversation(String conversationId, String userId) {
        applicationService.closeConversation(UUID.fromString(conversationId), UUID.fromString(userId));
    }

    @Override
    public void deleteConversation(String conversationId, String userId) {
        applicationService.deleteConversation(UUID.fromString(conversationId), UUID.fromString(userId));
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
            conversation.getId().toString(),
            conversation.getUserId().toString(),
            conversation.getTitle(),
            conversation.getStatus().name(),
            conversation.getResumeVersionId() != null ? conversation.getResumeVersionId().toString() : null,
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
            message.getId().toString(),
            message.getRole().name(),
            message.getContent(),
            message.getSequence(),
            message.getCreatedAt()
        );
    }
}
