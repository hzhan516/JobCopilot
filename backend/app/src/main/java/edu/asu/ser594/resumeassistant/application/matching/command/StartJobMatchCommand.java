package edu.asu.ser594.resumeassistant.application.matching.command;

import lombok.Builder;

import java.util.UUID;

/**
 * 启动职位匹配命令
 * Start job match command
 *
 * @param userId          用户ID / User ID
 * @param resumeVersionId 简历版本ID / Resume version ID
 * @param query           用户查询词 / User query
 * @param topK            期望返回的最大数量 / Expected maximum number of results
 */
@Builder
public record StartJobMatchCommand(
        UUID userId,
        String resumeVersionId,
        String query,
        Integer topK
) {
}
