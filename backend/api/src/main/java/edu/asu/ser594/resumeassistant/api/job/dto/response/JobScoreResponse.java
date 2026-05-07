package edu.asu.ser594.resumeassistant.api.job.dto.response;

/**
 * 职位评分响应
 * Response for a job scoring request.
 *
 * @param suitable   是否适合 / Whether the resume is suitable for the job.
 * @param summary    评分摘要 / A summary explaining the score.
 * @param finalScore 最终得分 (0.0 ~ 1.0) / The final suitability score.
 * @param breakdown  得分细项 / Score breakdown details.
 */
public record JobScoreResponse(
        boolean suitable,
        String summary,
        float finalScore,
        ScoreBreakdown breakdown
) {
    /**
     * 评分细项 DTO
     * Score breakdown DTO.
     *
     * @param skillScore      技能匹配得分 / Skill match score.
     * @param experienceScore 经验匹配得分 / Experience match score.
     * @param overallScore    综合得分 / Overall score.
     */
    public record ScoreBreakdown(
            float skillScore,
            float experienceScore,
            float overallScore
    ) {
    }
}
