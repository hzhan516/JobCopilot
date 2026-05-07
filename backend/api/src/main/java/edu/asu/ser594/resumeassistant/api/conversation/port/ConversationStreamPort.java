package edu.asu.ser594.resumeassistant.api.conversation.port;

/**
 * 对话流式回复端口接口
 * Conversation stream reply port.
 * <p>
 * 定义应用层与流式基础设施之间的边界，由 Infrastructure 层实现。
 * Defines the boundary between the application layer and streaming infrastructure,
 * implemented by the infrastructure layer.
 */
public interface ConversationStreamPort {

    /**
     * 完成指定对话的 AI 流式回复
     * Complete AI stream reply for the specified conversation.
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param content        AI 回复内容 / AI reply content
     */
    void completeReply(String conversationId, String content);

    /**
     * 标记指定对话的 AI 流式回复失败
     * Mark AI stream reply as failed for the specified conversation.
     *
     * @param conversationId 对话 ID / Conversation ID
     * @param errorMessage   错误信息 / Error message
     */
    void failReply(String conversationId, String errorMessage);
}
