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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Anti-corruption layer that shields the HTTP trigger layer from domain conversation concepts.
 * 防腐层，将领域层的对话概念转换为 HTTP 触发层可消费的 DTO，避免控制器直接依赖聚合根。
 */
@Component
@RequiredArgsConstructor
public class ConversationFacadeImpl implements ConversationFacade {

    private final ConversationApplicationService applicationService;
    private final ConversationQueryService queryService;

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request, UUID userId) {
        UUID resumeVersionId = request.resumeVersionId() != null && !request.resumeVersionId().isEmpty()
                ? UUID.fromString(request.resumeVersionId())
                : null;
        UUID jobId = request.jobId() != null && !request.jobId().isEmpty()
                ? UUID.fromString(request.jobId())
                : null;
        CreateConversationCommand command = CreateConversationCommand.builder()
                .userId(userId)
                .title(request.title())
                .resumeVersionId(resumeVersionId)
                .jobId(jobId)
                .build();

        Conversation conversation = applicationService.createConversation(command);
        return mapToResponse(conversation);
    }

    @Override
    public ConversationResponse sendMessage(String conversationId, SendMessageRequest request, UUID userId) {
        SendMessageCommand command = SendMessageCommand.builder()
                .conversationId(UUID.fromString(conversationId))
                .userId(userId)
                .role(MessageRole.USER) // client-facing endpoint only accepts user messages | 面向客户端的端点仅接受用户消息
                .content(request.content())
                .fileUrls(request.fileUrls())
                .build();

        Conversation conversation = applicationService.sendMessage(command);
        return mapToResponse(conversation);
    }

    @Override
    public ConversationResponse getConversation(String conversationId, UUID userId, Integer page, Integer size) {
        GetConversationQuery query = new GetConversationQuery(
                UUID.fromString(conversationId),
                userId,
                page,
                size
        );
        Conversation conversation = queryService.getConversation(query);
        return mapToResponse(conversation, page, size);
    }

    @Override
    public List<ConversationResponse> listConversations(UUID userId) {
        ListConversationsQuery query = new ListConversationsQuery(userId);
        return queryService.listConversations(query).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void closeConversation(String conversationId, UUID userId) {
        applicationService.closeConversation(UUID.fromString(conversationId), userId);
    }

    @Override
    public void deleteConversation(String conversationId, UUID userId) {
        applicationService.deleteConversation(UUID.fromString(conversationId), userId);
    }

    @Override
    public void saveAiReply(String conversationId, String content, String fileUrl, String aiOptimizedMarkdown) {
        applicationService.saveAiReply(UUID.fromString(conversationId), content, fileUrl, aiOptimizedMarkdown);
    }

    @Override
    public void completeAiReply(String conversationId, String content) {
        applicationService.completeAiReply(UUID.fromString(conversationId), content);
    }

    @Override
    public void failAiReply(String conversationId, String errorMessage) {
        applicationService.failAiReply(UUID.fromString(conversationId), errorMessage);
    }

    @Override
    public String uploadAttachment(String conversationId, MultipartFile file, UUID userId) {
        try {
            return applicationService.uploadAttachment(
                    UUID.fromString(conversationId),
                    userId,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file / 读取上传文件失败", e);
        }
    }

    /**
     * Converts the domain aggregate to a serializable response.
     * 将领域聚合根转换为可序列化的响应对象，包含消息分页防御（空分页参数透传全量列表）。
     */
    private ConversationResponse mapToResponse(Conversation conversation, Integer page, Integer size) {
        List<MessageResponse> messageResponses = applyMessagePagination(conversation.getMessages(), page, size)
                .stream()
                .map(this::mapMessageToResponse)
                .toList();

        return new ConversationResponse(
                conversation.getId().toString(),
                conversation.getUserId().toString(),
                conversation.getTitle(),
                conversation.getStatus().name(),
                conversation.getResumeVersionId() != null ? conversation.getResumeVersionId().toString() : null,
                conversation.getJobId() != null ? conversation.getJobId().toString() : null,
                messageResponses,
                conversation.getCreatedAt().atOffset(ZoneOffset.UTC),
                conversation.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }


    private ConversationResponse mapToResponse(Conversation conversation) {
        return mapToResponse(conversation, null, null);
    }

    /**
     * Best-effort subList pagination; out-of-range requests yield an empty list instead of throwing.
     * 尽力而为的子列表分页；越界请求返回空列表而非抛异常，避免前端因边界页码崩溃。
     */
    private List<edu.asu.ser594.resumeassistant.domain.conversation.entity.Message> applyMessagePagination(
            List<edu.asu.ser594.resumeassistant.domain.conversation.entity.Message> messages,
            Integer page, Integer size) {
        if (page == null || size == null || page < 0 || size <= 0) {
            return messages;
        }
        int fromIndex = page * size;
        if (fromIndex >= messages.size()) {
            return java.util.Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + size, messages.size());
        return messages.subList(fromIndex, toIndex);
    }


    private MessageResponse mapMessageToResponse(Message message) {
        return new MessageResponse(
                message.getId().toString(),
                message.getRole().name(),
                message.getContent(),
                message.getSequence(),
                message.getFileUrl(),
                message.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
