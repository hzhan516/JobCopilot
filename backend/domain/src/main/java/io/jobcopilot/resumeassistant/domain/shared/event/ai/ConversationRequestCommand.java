package io.jobcopilot.resumeassistant.domain.shared.event.ai;

import java.util.List;
import java.util.Map;

/**
 * 对话 AI 请求命令
 * Conversation AI request command
 *
 * @param conversationId  对话 ID / Conversation ID
 * @param userId          用户 ID / User ID
 * @param messageHistory  历史消息列表 / Message history list
 * @param currentMessage  当前用户消息 / Current user message
 * @param fileUrls        关联文件 URL 列表 / Associated file URL list
 * @param resumeVersionId 关联简历版本 ID / Associated resume version ID
 * @param resumeText      简历 Markdown 全文 / Full resume markdown text
 * @param primaryJobText  当前职位 JD 文本 / Primary job description text
 * @param relatedJobTexts 相关职位 JD 文本列表 / Related job description text list
 * @param init            是否为首次初始化 / Whether this is the first initialization
 * @param locale          用户界面语言 / User interface locale (e.g. en, zh-CN, zh-TW)
 * @param requestId       Request-level idempotency ID
 */
public record ConversationRequestCommand(
        String conversationId,
        String userId,
        List<Map<String, Object>> messageHistory,
        String currentMessage,
        List<String> fileUrls,
        String resumeVersionId,
        String resumeText,
        String primaryJobText,
        List<String> relatedJobTexts,
        boolean init,
        String locale,
        String requestId
) {
}
