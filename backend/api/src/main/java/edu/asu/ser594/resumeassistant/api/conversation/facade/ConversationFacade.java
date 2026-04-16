package edu.asu.ser594.resumeassistant.api.conversation.facade;

import edu.asu.ser594.resumeassistant.api.conversation.dto.ConversationResponse;
import edu.asu.ser594.resumeassistant.api.conversation.dto.CreateConversationRequest;
import edu.asu.ser594.resumeassistant.api.conversation.dto.SendMessageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 对话外观接口
 * Conversation facade interface
 */
public interface ConversationFacade {

    /**
     * 创建新对话
     * Create a new conversation
     */
    ConversationResponse createConversation(CreateConversationRequest request, UUID userId);

    /**
     * 发送消息
     * Send a message
     */
    ConversationResponse sendMessage(String conversationId, SendMessageRequest request, UUID userId);

    /**
     * 获取对话详情
     * Get conversation details
     */
    ConversationResponse getConversation(String conversationId, UUID userId);

    /**
     * 获取用户的所有对话
     * List all conversations for user
     */
    List<ConversationResponse> listConversations(UUID userId);

    /**
     * 关闭对话
     * Close conversation
     */
    void closeConversation(String conversationId, UUID userId);

    /**
     * 删除对话
     * Delete conversation
     */
    void deleteConversation(String conversationId, UUID userId);

    /**
     * 保存 AI 回复消息
     * Save AI reply message
     */
    void saveAiReply(String conversationId, String content, String fileUrl);

    /**
     * 上传对话附件到 MinIO
     * Upload conversation attachment to MinIO
     */
    String uploadAttachment(String conversationId, MultipartFile file, UUID userId);
}
