package edu.asu.ser594.resumeassistant.domain.job.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 职位评分记录实体
 * Job score record entity.
 * <p>
 * 每次评分生成一条新记录，查询时按 createdAt 取最新。
 * A new record is created for each scoring; queries use createdAt to get the latest.
 */
@Getter
public class JobScoreRecord implements Entity<String> {

    private final String id;
    private final String jobId;
    private final String resumeVersionId;
    private final UUID userId;
    private final Boolean suitable;
    private final Float finalScore;
    private final Float skillScore;
    private final Float experienceScore;
    private final Float overallScore;
    private final String summary;
    private final LocalDateTime createdAt;

    public JobScoreRecord(String id, String jobId, String resumeVersionId, UUID userId,
                          Boolean suitable, Float finalScore, Float skillScore,
                          Float experienceScore, Float overallScore, String summary,
                          LocalDateTime createdAt) {
        this.id = id;
        this.jobId = jobId;
        this.resumeVersionId = resumeVersionId;
        this.userId = userId;
        this.suitable = suitable;
        this.finalScore = finalScore;
        this.skillScore = skillScore;
        this.experienceScore = experienceScore;
        this.overallScore = overallScore;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    /**
     * 创建新的评分记录
     * Creates a new job score record.
     */
    public static JobScoreRecord create(String jobId, String resumeVersionId, UUID userId,
                                        Boolean suitable, Float finalScore, Float skillScore,
                                        Float experienceScore, Float overallScore, String summary) {
        return new JobScoreRecord(
                UUID.randomUUID().toString(),
                jobId,
                resumeVersionId,
                userId,
                suitable,
                finalScore,
                skillScore,
                experienceScore,
                overallScore,
                summary,
                LocalDateTime.now()
        );
    }

    @Override
    public String getId() {
        return id;
    }
}
