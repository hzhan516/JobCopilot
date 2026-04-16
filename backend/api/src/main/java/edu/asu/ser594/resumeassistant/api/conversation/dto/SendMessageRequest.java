package edu.asu.ser594.resumeassistant.api.conversation.dto;

/**
 * 发送消息请求 DTO
 * Send message request DTO
 *
 * @param content 消息内容 / Message content
 */
public record SendMessageRequest(
    String content
) {
    public SendMessageRequest {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空 / Message content cannot be empty");
        }
    }
}
