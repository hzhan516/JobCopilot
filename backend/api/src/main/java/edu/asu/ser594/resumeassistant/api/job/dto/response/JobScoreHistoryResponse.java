package edu.asu.ser594.resumeassistant.api.job.dto.response;

import java.time.LocalDateTime;

/**
 * 职位评分历史响应
 * Job score history response.
 *
 * @param id              评分记录ID / Score record ID
 * @param jobId           职位ID / Job ID
 * @param resumeVersionId 简历版本ID / Resume version ID
 * @param suitable        是否适合 / Whether suitable
 * @param finalScore      最终得分 (0.0 ~ 1.0) / Final score
 * @param skillScore      技能匹配得分 / Skill match score
 * @param experienceScore 经验匹配得分 / Experience match score
 * @param overallScore    综合得分 / Overall score
 * @param summary         评分摘要 / Score summary
 * @param createdAt       创建时间 / Created at
 */
public record JobScoreHistoryResponse(
        String id,
        String jobId,
        String resumeVersionId,
        boolean suitable,
        float finalScore,
        float skillScore,
        float experienceScore,
        float overallScore,
        String summary,
        LocalDateTime createdAt
) {
}
