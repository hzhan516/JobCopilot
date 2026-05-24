package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.api.job.dto.response.JobScoreResponse;
import io.jobcopilot.resumeassistant.domain.job.entity.JobScoreRecord;
import io.jobcopilot.resumeassistant.domain.job.repository.JobScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Persists AI scoring results into the job score repository.
 * 将 AI 评分结果持久化到职位评分仓库。
 */
@Component
@RequiredArgsConstructor
public class JobScoringResultSaver {

    private final JobScoreRepository jobScoreRepository;

    @Transactional
    public JobScoreResponse save(String jobId, UUID userId, String resumeVersionId,
                                  Map<String, Object> aiResponse) {
        boolean suitable = Boolean.TRUE.equals(aiResponse.get("suitable"));
        String summary = (String) aiResponse.get("summary");
        float finalScore = extractFloat(aiResponse, "finalScore");

        @SuppressWarnings("unchecked")
        Map<String, Object> breakdown = (Map<String, Object>) aiResponse.get("breakdown");
        float skillScore = 0.0f, experienceScore = 0.0f, overallScore = 0.0f;
        if (breakdown != null) {
            skillScore = extractFloat(breakdown, "skillScore");
            experienceScore = extractFloat(breakdown, "experienceScore");
            overallScore = extractFloat(breakdown, "overallScore");
        }

        JobScoreRecord record = JobScoreRecord.create(
                jobId, resumeVersionId, userId, suitable,
                finalScore, skillScore, experienceScore, overallScore, summary);
        jobScoreRepository.save(record);

        return new JobScoreResponse(suitable, summary != null ? summary : "", finalScore,
                new JobScoreResponse.ScoreBreakdown(skillScore, experienceScore, overallScore));
    }

    private static float extractFloat(Map<String, Object> map, String key) {
        Number n = (Number) map.get(key);
        return n != null ? n.floatValue() : 0.0f;
    }
}
