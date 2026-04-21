package edu.asu.ser594.resumeassistant.api.job.dto.response;

import java.util.List;

/**
 * 职位匹配整体响应
 * Job match response
 *
 * @param matchId 匹配任务ID（异步模式返回） / Match task ID (returned in async mode)
 * @param status 匹配状态: PROCESSING, COMPLETED, FAILED / Match status
 * @param matches 匹配到的职位列表 / List of matched jobs
 * @param total 匹配总数 / Total matched count
 * @param recallTime 召回耗时(ms) / Recall phase time (ms)
 * @param rankTime 精排耗时(ms) / Ranking phase time (ms)
 */
public record JobMatchResponse(
        String matchId,
        String status,
        List<MatchItem> matches,
        Integer total,
        Long recallTime,
        Long rankTime
) {
    /**
     * 构建处理中响应
     * Build processing response
     *
     * @param matchId 匹配ID / Match ID
     * @return 处理中响应 / Processing response
     */
    public static JobMatchResponse processing(final String matchId) {
        return new JobMatchResponse(matchId, "PROCESSING", List.of(), 0, null, null);
    }

    /**
     * 构建完成响应
     * Build completed response
     *
     * @param matchId 匹配ID / Match ID
     * @param matches 匹配列表 / Match list
     * @param total 总数 / Total count
     * @param recallTime 召回耗时 / Recall time
     * @param rankTime 精排耗时 / Rank time
     * @return 完成响应 / Completed response
     */
    public static JobMatchResponse completed(final String matchId,
                                             final List<MatchItem> matches,
                                             final Integer total,
                                             final Long recallTime,
                                             final Long rankTime) {
        return new JobMatchResponse(matchId, "COMPLETED", matches, total, recallTime, rankTime);
    }
}
