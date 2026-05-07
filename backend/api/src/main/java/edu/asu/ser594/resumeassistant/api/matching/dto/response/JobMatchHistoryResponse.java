package edu.asu.ser594.resumeassistant.api.matching.dto.response;

import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 职位匹配历史响应
 * Job match history response
 *
 * @param matchId         匹配记录ID / Match record ID
 * @param userId          用户ID / User ID
 * @param resumeVersionId 简历版本ID / Resume version ID
 * @param query           查询词 / Query
 * @param status          状态: PROCESSING, COMPLETED, FAILED / Status
 * @param matches         匹配结果列表 / Match result list
 * @param total           匹配总数 / Total matched count
 * @param recallTime      召回耗时(ms) / Recall phase time (ms)
 * @param rankTime        精排耗时(ms) / Ranking phase time (ms)
 * @param modelVersion    模型版本 / Model version
 * @param createdAt       创建时间 / Created at
 * @param completedAt     完成时间 / Completed at
 */
public record JobMatchHistoryResponse(
        String matchId,
        String userId,
        String resumeVersionId,
        String query,
        String status,
        List<MatchItem> matches,
        Integer total,
        Long recallTime,
        Long rankTime,
        String modelVersion,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
