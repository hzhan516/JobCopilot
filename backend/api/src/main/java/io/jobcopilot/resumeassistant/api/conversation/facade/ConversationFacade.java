package io.jobcopilot.resumeassistant.api.conversation.facade;

import io.jobcopilot.resumeassistant.api.conversation.dto.ConversationResponse;
import io.jobcopilot.resumeassistant.api.conversation.dto.CreateConversationRequest;
import io.jobcopilot.resumeassistant.api.conversation.dto.SendMessageRequest;
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
    ConversationResponse getConversation(String conversationId, UUID userId, Integer page, Integer size);

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
     *
     * @param aiOptimizedMarkdown AI 优化后的简历 Markdown（可选）/ AI optimized resume markdown (optional)
     */
    void saveAiReply(String conversationId, String content, String fileUrl, String aiOptimizedMarkdown);

    /**
     * 完成 AI 流式回复并唤醒等待中的流请求
     * Complete AI stream reply and wake up pending stream request.
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param content        AI 回复内容 / AI reply content
     */
    void completeAiReply(String conversationId, String content);

    /**
     * 标记 AI 流式回复失败并释放等待中的流连接
     * Mark AI stream reply as failed and release pending stream connection.
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param errorMessage   错误信息 / Error message
     */
    void failAiReply(String conversationId, String errorMessage);

    /**
     * Resolve an AI failure code into user-readable text for the requested locale.
     *
     * @param errorCode AI failure code
     * @param localeTag BCP 47 locale tag
     */
    String resolveAiFailureMessage(String errorCode, String localeTag);

    /**
     * 上传对话附件到 MinIO
     * Upload conversation attachment to MinIO
     */
    String uploadAttachment(String conversationId, MultipartFile file, UUID userId);
}
