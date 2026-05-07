package edu.asu.ser594.resumeassistant.trigger.http.controller.conversation;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.infrastructure.messaging.stream.ConversationStreamService;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
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
 * 对话控制器
 * Conversation HTTP Controller
 */
@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationFacade conversationFacade;
    private final ConversationStreamService streamService;

    /**
     * 创建对话
     * Create conversation
     */
    @PostMapping
    public ApiResponse<ConversationResponse> createConversation(
            @Validated @RequestBody CreateConversationRequest request,
            @CurrentUser UUID userId) {
        log.info("REST request to create conversation for user: {}", userId);
        return ApiResponse.success(conversationFacade.createConversation(request, userId));
    }

    /**
     * 发送消息
     * Send message
     */
    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ConversationResponse> sendMessage(
            @PathVariable String conversationId,
            @Validated @RequestBody SendMessageRequest request,
            @CurrentUser UUID userId) {
        log.info("REST request to send message to conversation: {}", conversationId);
        return ApiResponse.success(conversationFacade.sendMessage(conversationId, request, userId));
    }

    /**
     * 获取对话详情（支持消息分页）
     * Get conversation details with optional message pagination
     */
    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @CurrentUser UUID userId) {
        log.info("REST request to get conversation: {}, page: {}, size: {}", conversationId, page, size);
        return ApiResponse.success(conversationFacade.getConversation(conversationId, userId, page, size));
    }

    /**
     * 获取当前用户的所有对话
     * List user conversations
     */
    @GetMapping
    public ApiResponse<List<ConversationResponse>> listConversations(
            @CurrentUser UUID userId) {
        log.info("REST request to list conversations for user: {}", userId);
        return ApiResponse.success(conversationFacade.listConversations(userId));
    }

    /**
     * 关闭对话
     * Close conversation
     */
    @PutMapping("/{conversationId}/close")
    public ApiResponse<Void> closeConversation(
            @PathVariable String conversationId,
            @CurrentUser UUID userId) {
        log.info("REST request to close conversation: {}", conversationId);
        conversationFacade.closeConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 删除对话
     * Delete conversation
     */
    @DeleteMapping("/{conversationId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable String conversationId,
            @CurrentUser UUID userId) {
        log.info("REST request to delete conversation: {}", conversationId);
        conversationFacade.deleteConversation(conversationId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 上传对话附件（AI 生成文件等）
     * Upload conversation attachment (e.g. AI-generated files)
     */
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
     * 流式获取 AI 回复
     * Stream AI reply (pseudo-streaming via MQ bridge)
     *
     * <p>当前为伪流式传输：AI Service 生成完整回复后通过 MQ 一次性推送，
     * 后端保持 HTTP 连接等待后将完整内容写入响应流。前端可显示 loading 状态。</p>
     *
     * <p>调用时序：先 POST /messages 发送消息，再 GET /stream 等待回复。</p>
     */
    @GetMapping(value = "/{conversationId}/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> streamAiReply(
            @PathVariable String conversationId,
            @CurrentUser UUID userId) {
        log.info("REST stream request for conversation: {}", conversationId);

        // 所有权校验 / Ownership verification
        conversationFacade.getConversation(conversationId, userId, null, null);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(outputStream -> {
                    try {
                        String reply = streamService.awaitReply(conversationId);
                        if (reply != null) {
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
                            // best effort
                        }
                    }
                });
    }
}
