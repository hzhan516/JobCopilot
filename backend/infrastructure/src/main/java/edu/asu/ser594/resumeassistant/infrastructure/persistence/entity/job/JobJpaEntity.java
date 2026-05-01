package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job;

import edu.asu.ser594.resumeassistant.domain.job.valueobject.JobStatus;
import edu.asu.ser594.resumeassistant.domain.job.valueobject.ParsedJobContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/** 职位 JPA 实体 / Job JPA entity */
@Entity
@Table(name = "jobs")
@Getter
@Setter
public class JobJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "image_check_enabled", nullable = false)
    private boolean imageCheckEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_content", columnDefinition = "jsonb")
    private ParsedJobContent parsedContent;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 持久化前设置时间戳 / Set timestamps before persist */
    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /** 更新前刷新时间戳 / Refresh timestamp before update */
    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
