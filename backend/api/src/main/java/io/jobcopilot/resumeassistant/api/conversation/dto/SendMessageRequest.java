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
        fileUrls = fileUrls == null ? List.of() : List.copyOf(fileUrls);
        if (fileUrls.size() > 3) {
            throw new IllegalArgumentException("At most 3 attachments are allowed");
        }
        if (fileUrls.stream().anyMatch(url -> url == null || url.isBlank() || url.length() > 2048)) {
            throw new IllegalArgumentException("Invalid attachment reference");
        }
    }
}
