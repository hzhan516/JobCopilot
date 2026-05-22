package io.jobcopilot.resumeassistant.api.job.dto.response;

/**
 * 匹配因子详情
 * Match factors details
 *
 * @param skillMatch      技能匹配度 / Skill match score
 * @param experienceMatch 经验匹配度 / Experience match score
 * @param locationMatch   地理位置匹配度 / Location match score
 */
public record MatchFactors(
        Double skillMatch,
        Double experienceMatch,
        Double locationMatch
) {
}
