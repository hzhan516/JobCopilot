package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 职位评分记录 JPA 实体 / Job score record JPA entity
 */
@Entity
@Table(name = "job_scores")
@Getter
@Setter
public class JobScoreRecordJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    @Column(name = "resume_version_id", nullable = false, length = 64)
    private String resumeVersionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column
    private Boolean suitable;

    @Column(name = "final_score")
    private Float finalScore;

    @Column(name = "skill_score")
    private Float skillScore;

    @Column(name = "experience_score")
    private Float experienceScore;

    @Column(name = "overall_score")
    private Float overallScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
