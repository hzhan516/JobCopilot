package edu.asu.ser594.resumeassistant.application.matching.command;

import edu.asu.ser594.resumeassistant.api.job.dto.response.MatchItem;
import lombok.Builder;

import java.util.List;

/**
 * 保存匹配结果命令
 * Save match result command
 *
 * @param matchId 匹配任务ID / Match task ID
 * @param rankedResults 精排结果列表 / Ranked result list
 * @param rankTimeMs 精排耗时(毫秒) / Ranking time in ms
 */
@Builder
public record SaveMatchResultCommand(
        String matchId,
        List<MatchItem> rankedResults,
        Long rankTimeMs
) {}
