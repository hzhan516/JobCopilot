package edu.asu.ser594.resumeassistant.application.matching.query;

/**
 * 查询匹配结果查询对象
 * Get match result query
 *
 * @param matchId 匹配任务ID / Match task ID
 */
public record GetMatchResultQuery(
        String matchId
) {}
