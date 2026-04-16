package edu.asu.ser594.resumeassistant.trigger.http.controller.conversation;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import edu.asu.ser594.resumeassistant.api.conversation.facade.ConversationFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话控制器
 * Conversation HTTP Controller
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private final ConversationFacade conversationFacade;

    /**
     * 创建对话
     * Create conversation
     */
    @PostMapping
    public ApiResponse<ConversationResponse> createConversation(
            @Validated @RequestBody CreateConversationRequest request,
            @CurrentUser String userId) {
        log.info("REST request to create conversation for user: {}", userId);
        return conversationFacade.createConversation(request, userId);
    }

    /**
     * 发送消息
     * Send message
     */
    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ConversationResponse> sendMessage(
            @PathVariable String conversationId,
            @Validated @RequestBody SendMessageRequest request,
            @CurrentUser String userId) {
        log.info("REST request to send message to conversation: {}", conversationId);
        return conversationFacade.sendMessage(conversationId, request, userId);
    }

    /**
     * 获取对话详情
     * Get conversation details
     */
    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @CurrentUser String userId) {
        log.info("REST request to get conversation: {}", conversationId);
        return conversationFacade.getConversation(conversationId, userId);
    }

    /**
     * 获取当前用户的所有对话
     * List user conversations
     */
    @GetMapping
    public ApiResponse<List<ConversationResponse>> listConversations(
            @CurrentUser String userId) {
        log.info("REST request to list conversations for user: {}", userId);
        return conversationFacade.listConversations(userId);
    }

    /**
     * 关闭对话
     * Close conversation
     */
    @PutMapping("/{conversationId}/close")
    public ApiResponse<Void> closeConversation(
            @PathVariable String conversationId,
            @CurrentUser String userId) {
        log.info("REST request to close conversation: {}", conversationId);
        return conversationFacade.closeConversation(conversationId, userId);
    }

    /**
     * 删除对话
     * Delete conversation
     */
    @DeleteMapping("/{conversationId}")
    public ApiResponse<Void> deleteConversation(
            @PathVariable String conversationId,
            @CurrentUser String userId) {
        log.info("REST request to delete conversation: {}", conversationId);
        return conversationFacade.deleteConversation(conversationId, userId);
    }
}
