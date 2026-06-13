package io.jobcopilot.resumeassistant.trigger.http.controller.conversation;

import io.jobcopilot.resumeassistant.api.common.dto.ApiResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.ConversationResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.CreateConversationRequest;
import io.jobcopilot.resumeassistant.api.conversation.dto.SendMessageRequest;
import io.jobcopilot.resumeassistant.api.conversation.facade.ConversationFacade;
import io.jobcopilot.resumeassistant.infrastructure.messaging.stream.ConversationStreamService;
import io.jobcopilot.resumeassistant.trigger.http.security.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for the AI copilot conversation lifecycle, including a pseudo-streaming endpoint
 * that bridges synchronous HTTP clients with asynchronous MQ-based AI workers.
 * AI 助手对话生命周期的 REST 端点，包含伪流式端点，用于桥接同步 HTTP 客户端与基于 MQ 的异步 AI 工作线程
 */
@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationFacade conversationFacade;
    private final ConversationStreamService streamService;

    @PostMapping
    public ApiResponse<ConversationResponse> createConversation(
            @Validated @RequestBody CreateConversationRequest request,
            @CurrentUser UUID userId) {
        log.info("REST request to create conversation for user: {}", userId);
        return ApiResponse.success(conversationFacade.createConversation(request, userId));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ConversationResponse> sendMessage(
            @PathVariable String conversationId,
            @Validated @RequestBody SendMessageRequest request,
            @CurrentUser UUID userId) {
        log.info("REST request to send message to conversation: {}", conversationId);
        return ApiResponse.success(conversationFacade.sendMessage(conversationId, request, userId));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @CurrentUser UUID userId) {
        log.info("REST request to get conversation: {}, page: {}, size: {}", conversationId, page, size);
        return ApiResponse.success(conversationFacade.getConversation(conversationId, userId, page, size));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> listConversations(
            @CurrentUser UUID userId) {
        log.info("REST request to list conversations for user: {}", userId);
        return ApiResponse.success(conversationFacade.listConversations(userId));
    }

    @PutMapping("/{conversationId}/close")
    public ApiResponse<Void> closeConversation(
            @PathVariable String conversationId,
            @CurrentUser UUID userId) {
        log.info("REST request to close conversation: {}", conversationId);
        conversationFacade.closeConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{conversationId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable String conversationId,
            @CurrentUser UUID userId) {
        log.info("REST request to delete conversation: {}", conversationId);
        conversationFacade.deleteConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/{conversationId}/files", consumes = "multipart/form-data")
    public ApiResponse<String> uploadAttachment(
            @PathVariable String conversationId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId) {
        log.info("REST request to upload attachment for conversation: {}", conversationId);
        String fileUrl = conversationFacade.uploadAttachment(conversationId, file, userId);
        return ApiResponse.success(fileUrl);
    }

    /**
     * Pseudo-streaming endpoint that keeps the HTTP connection open until the AI worker publishes
     * the complete reply via RabbitMQ. The client should call POST /messages first, then poll this endpoint.
     * 伪流式端点，保持 HTTP 连接打开直至 AI 工作线程通过 RabbitMQ 发布完整回复。客户端应先调用 POST /messages，再轮询此端点
     */
    @GetMapping(value = "/{conversationId}/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> streamAiReply(
            @PathVariable String conversationId,
            @CurrentUser UUID userId,
            HttpServletResponse response) {
        log.info("REST stream request for conversation: {}", conversationId);

        // verify ownership before blocking on the stream | 在阻塞等待流之前先校验所有权，防止跨用户流劫持
        conversationFacade.getConversation(conversationId, userId, null, null);

        // defense-in-depth security headers | 纵深防御安全头
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(outputStream -> {
                    try {
                        String reply = streamService.awaitReply(conversationId);
                        if (reply != null) {
                            // Strip HTML tags as defense-in-depth; Content-Type is text/plain
                            reply = reply.replaceAll("<[^>]*>", "");
                            outputStream.write(reply.getBytes(StandardCharsets.UTF_8));
                        } else {
                            String timeoutMsg = "AI reply timed out. Please try again later.\n" +
                                    "AI 回复超时，请稍后重试。";
                            outputStream.write(timeoutMsg.getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        log.error("Error streaming reply for conversation: {}", conversationId, e);
                        String errorMsg = "Failed to retrieve AI reply.\n获取 AI 回复失败。";
                        try {
                            outputStream.write(errorMsg.getBytes(StandardCharsets.UTF_8));
                        } catch (Exception ignored) {
                            // best effort | 尽力写入，忽略次级异常
                        }
                    }
                });
    }
}
