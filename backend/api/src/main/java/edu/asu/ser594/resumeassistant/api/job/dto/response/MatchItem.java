package edu.asu.ser594.resumeassistant.api.job.dto.response;

/**
 * 单个职位匹配结果项
 * Single job match item
 *
 * @param jobId        职位ID / Job ID
 * @param title        职位标题 / Job title
 * @param company      公司名称 / Company name
 * @param matchScore   综合匹配得分 / Overall match score
 * @param matchFactors 详细匹配因子 / Detailed match factors
 * @param description  职位描述片段 / Job description snippet
 * @param matchReason  AI生成的匹配理由 / AI generated match reason
 */
public record MatchItem(
        String jobId,
        String title,
        String company,
        Double matchScore,
        MatchFactors matchFactors,
        String description,
        String matchReason
) {
}
