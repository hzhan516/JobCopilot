package edu.asu.ser594.resumeassistant.application.matching.query;

import java.util.UUID;

/**
 * 查询匹配历史查询对象
 * List match history query
 *
 * @param userId 用户ID / User ID
 */
public record ListMatchHistoryQuery(
        UUID userId
) {
}
