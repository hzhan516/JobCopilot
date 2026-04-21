package edu.asu.ser594.resumeassistant.application.conversation.query;

import java.util.UUID;

/**
 * 获取用户对话列表查询
 * List user conversations query
 */
public record ListConversationsQuery(
    UUID userId
) {}
