package edu.asu.ser594.resumeassistant.api.conversation.facade;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;

import java.util.List;

/**
 * 对话外观接口
 * Conversation facade interface
 */
public interface ConversationFacade {

    /**
     * 创建新对话
     * Create a new conversation
     */
    ApiResponse<ConversationResponse> createConversation(CreateConversationRequest request, String userId);

    /**
     * 发送消息
     * Send a message
     */
    ApiResponse<ConversationResponse> sendMessage(String conversationId, SendMessageRequest request, String userId);

    /**
     * 获取对话详情
     * Get conversation details
     */
    ApiResponse<ConversationResponse> getConversation(String conversationId, String userId);

    /**
     * 获取用户的所有对话
     * List all conversations for user
     */
    ApiResponse<List<ConversationResponse>> listConversations(String userId);

    /**
     * 关闭对话
     * Close conversation
     */
    ApiResponse<Void> closeConversation(String conversationId, String userId);

    /**
     * 删除对话
     * Delete conversation
     */
    ApiResponse<Void> deleteConversation(String conversationId, String userId);
}
