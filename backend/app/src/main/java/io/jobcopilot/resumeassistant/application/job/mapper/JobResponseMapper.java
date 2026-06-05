package io.jobcopilot.resumeassistant.application.job.mapper;

import io.jobcopilot.resumeassistant.api.job.dto.response.JobResponse;
import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreHistoryResponse;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.entity.JobScoreRecord;

import java.time.ZoneOffset;

/**
 * Maps Job and JobScoreRecord domain entities to API response DTOs.
 * 将 Job 和 JobScoreRecord 领域实体映射为 API 响应 DTO。
 */
public final class JobResponseMapper {

    private JobResponseMapper() {}

    public static JobResponse toResponse(Job job) {
        JobResponse.ParsedJobContentResponse parsedContentResponse = null;
        if (job.getParsedContent() != null) {
            parsedContentResponse = new JobResponse.ParsedJobContentResponse(
                    job.getParsedContent().title(),
                    job.getParsedContent().company(),
                    job.getParsedContent().salary(),
                    job.getParsedContent().location(),
                    job.getParsedContent().description(),
                    job.getParsedContent().requirements()
            );
        }

        return new JobResponse(
                job.getId(),
                job.getUserId().toString(),
                job.getOriginalUrl(),
                job.getStatus().name(),
                parsedContentResponse,
                job.isImageCheckEnabled(),
                job.getErrorMessage()
        );
    }

    public static JobScoreHistoryResponse toScoreHistoryResponse(JobScoreRecord record) {
        return new JobScoreHistoryResponse(
                record.getId(),
                record.getJobId(),
                record.getResumeVersionId(),
                Boolean.TRUE.equals(record.getSuitable()),
                record.getFinalScore() != null ? record.getFinalScore() : 0.0f,
                record.getSkillScore() != null ? record.getSkillScore() : 0.0f,
                record.getExperienceScore() != null ? record.getExperienceScore() : 0.0f,
                record.getOverallScore() != null ? record.getOverallScore() : 0.0f,
                record.getSummary(),
                record.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }
}
