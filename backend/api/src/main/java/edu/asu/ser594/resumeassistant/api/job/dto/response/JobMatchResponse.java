package edu.asu.ser594.resumeassistant.api.job.dto.response;

import java.util.List;

/**
 * 职位匹配整体响应
 * Job match response
 *
 * @param matches 匹配到的职位列表 / List of matched jobs
 * @param total 匹配总数 / Total matched count
 * @param recallTime 召回耗时(ms) / Recall phase time (ms)
 * @param rankTime 精排耗时(ms) / Ranking phase time (ms)
 */
public record JobMatchResponse(
        List<MatchItem> matches,
        Integer total,
        Long recallTime,
        Long rankTime
) {}
