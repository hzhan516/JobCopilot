package edu.asu.ser594.resumeassistant.api.job.dto.request;

/**
 * 职位评分请求
 * Request to score a job against a resume.
 *
 * @param resumeVersionId 简历版本 ID / The resume version ID.
 */
public record JobScoreRequest(
        String resumeVersionId
) {
}
