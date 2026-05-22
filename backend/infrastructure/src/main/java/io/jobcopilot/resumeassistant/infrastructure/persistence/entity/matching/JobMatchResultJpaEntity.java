package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.matching;

import io.jobcopilot.resumeassistant.domain.matching.valueobject.MatchStatus;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RankedJob;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位匹配结果 JPA 实体
 * Job match result JPA entity
 */
@Entity
@Table(name = "job_match_results")
@Getter
@Setter
public class JobMatchResultJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "resume_version_id", length = 64)
    private String resumeVersionId;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MatchStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recall_results", columnDefinition = "jsonb")
    private List<RecallResult> recallResults;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ranked_results", columnDefinition = "jsonb")
    private List<RankedJob> rankedResults;

    @Column(name = "recall_time_ms")
    private Long recallTimeMs;

    @Column(name = "rank_time_ms")
    private Long rankTimeMs;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
