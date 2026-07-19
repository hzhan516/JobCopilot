package io.jobcopilot.resumeassistant.application.conversation;

import io.jobcopilot.resumeassistant.api.conversation.dto.ConversationResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.AiReplyResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.CreateConversationRequest;
import io.jobcopilot.resumeassistant.api.conversation.dto.MessageResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.SendMessageRequest;
import io.jobcopilot.resumeassistant.api.conversation.facade.ConversationFacade;
import io.jobcopilot.resumeassistant.application.conversation.command.CreateConversationCommand;
import io.jobcopilot.resumeassistant.application.conversation.command.SendMessageCommand;
import io.jobcopilot.resumeassistant.application.conversation.query.GetConversationQuery;
import io.jobcopilot.resumeassistant.application.conversation.query.ListConversationsQuery;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationApplicationService;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationCompactionService;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationFailureMessageResolver;
import io.jobcopilot.resumeassistant.application.conversation.service.ConversationQueryService;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Conversation;
import io.jobcopilot.resumeassistant.domain.conversation.entity.Message;
import io.jobcopilot.resumeassistant.domain.conversation.valueobject.MessageRole;
import io.jobcopilot.resumeassistant.infrastructure.cache.config.DynamicConfigCache;
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
    private final ConversationCompactionService compactionService;
    private final ConversationFailureMessageResolver failureMessageResolver;

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
    public boolean saveAiReply(String conversationId, String requestId, String content, String fileUrl,
                               String aiOptimizedMarkdown, int promptTokens, int completionTokens) {
        return applicationService.saveAiReply(UUID.fromString(conversationId), UUID.fromString(requestId),
                content, fileUrl, aiOptimizedMarkdown, promptTokens, completionTokens);
    }

    @Override
    public boolean failAiReply(String conversationId, String requestId, String errorCode) {
        return applicationService.failAiReply(UUID.fromString(conversationId), UUID.fromString(requestId), errorCode);
    }

    @Override
    public ConversationResponse retryAiReply(String conversationId, UUID userId) {
        return mapToResponse(applicationService.retryAiReply(UUID.fromString(conversationId), userId));
    }

    @Override
    public void completeAiReply(String conversationId, String content) {
        applicationService.completeAiReply(UUID.fromString(conversationId), content);
    }

    @Override
    public void notifyAiReplyFailure(String conversationId, String errorMessage) {
        applicationService.notifyAiReplyFailure(UUID.fromString(conversationId), errorMessage);
    }

    @Override
    public String resolveAiFailureMessage(String errorCode, String localeTag) {
        return failureMessageResolver.resolve(errorCode, localeTag);
    }

    @Override
    public ConversationResponse compactConversation(String conversationId, UUID userId) {
        compactionService.requestCompaction(UUID.fromString(conversationId), userId);
        // Return current state so the frontend sees COMPACTING status immediately
        Conversation conversation = queryService.getConversation(
                new io.jobcopilot.resumeassistant.application.conversation.query.GetConversationQuery(
                        UUID.fromString(conversationId), userId, null, null));
        return mapToResponse(conversation);
    }

    @Override
    public boolean applyCompactionResult(String conversationId, String requestId, String summary,
                                         int throughSequence, int contextTokens) {
        return compactionService.applyCompactionResult(
                conversationId, requestId, summary, throughSequence, contextTokens);
    }

    @Override
    public boolean failCompaction(String conversationId, String requestId) {
        return compactionService.failCompaction(conversationId, requestId);
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
                conversation.getUpdatedAt().atOffset(ZoneOffset.UTC),
                conversation.getContextTokens(),
                readContextWindow(),
                computeUsageRatio(conversation.getContextTokens()),
                isCompactAdvised(conversation.getContextTokens()),
                mapAiReply(conversation)
        );
    }

    private AiReplyResponse mapAiReply(Conversation conversation) {
        var state = conversation.getAiReplyState();
        return new AiReplyResponse(
                state.requestId() != null ? state.requestId().toString() : null,
                state.status().name(),
                state.errorCode(),
                state.startedAt() != null ? state.startedAt().atOffset(ZoneOffset.UTC) : null,
                state.completedAt() != null ? state.completedAt().atOffset(ZoneOffset.UTC) : null,
                state.userMessageSequence(),
                state.assistantMessageSequence()
        );
    }


    private ConversationResponse mapToResponse(Conversation conversation) {
        return mapToResponse(conversation, null, null);
    }

    private static int readContextWindow() {
        String cached = DynamicConfigCache.get("chat.contextWindow");
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 1_000_000;
    }

    private double computeUsageRatio(int contextTokens) {
        int window = readContextWindow();
        if (window <= 0 || contextTokens <= 0) {
            return 0.0;
        }
        return (double) contextTokens / window;
    }

    private boolean isCompactAdvised(int contextTokens) {
        int window = readContextWindow();
        if (window <= 0) {
            return false;
        }
        int threshold = readCompactThreshold();
        double ratio = (double) contextTokens / window;
        return ratio * 100 >= threshold;
    }

    private static int readCompactThreshold() {
        String cached = DynamicConfigCache.get("chat.compactThreshold");
        if (cached != null) {
            try {
                return Integer.parseInt(cached);
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 80;
    }

    /**
     * Best-effort subList pagination; out-of-range requests yield an empty list instead of throwing.
     * 尽力而为的子列表分页；越界请求返回空列表而非抛异常，避免前端因边界页码崩溃。
     */
    private List<io.jobcopilot.resumeassistant.domain.conversation.entity.Message> applyMessagePagination(
            List<io.jobcopilot.resumeassistant.domain.conversation.entity.Message> messages,
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
