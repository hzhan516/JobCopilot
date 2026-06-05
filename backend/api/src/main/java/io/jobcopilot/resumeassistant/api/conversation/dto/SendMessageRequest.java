package io.jobcopilot.resumeassistant.api.conversation.dto;

import java.util.List;

/**
 * 发送消息请求 DTO
 * Send message request DTO
 *
 * @param content  消息内容 / Message content
 * @param fileUrls 关联文件 URL 列表 / Associated file URL list
 */
public record SendMessageRequest(
        String content,
        List<String> fileUrls
) {
    public SendMessageRequest {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空 / Message content cannot be empty");
        }
    }
}
