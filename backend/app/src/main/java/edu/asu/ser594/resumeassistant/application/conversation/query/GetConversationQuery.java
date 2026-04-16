package edu.asu.ser594.resumeassistant.application.conversation.query;

import java.util.UUID;

/**
 * 获取对话查询
 * Get conversation query
 */
public record GetConversationQuery(
    UUID conversationId,
    UUID userId
) {}
