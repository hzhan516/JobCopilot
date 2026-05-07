package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.tracking;

import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 求职申请跟踪 JPA 实体
 * Application tracking JPA entity
 */
@Entity
@Table(name = "application_trackings")
@Getter
@Setter
public class ApplicationTrackingJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "job_id", length = 64)
    private String jobId;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    @Column(name = "applied_at")
    private LocalDate appliedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "events", columnDefinition = "jsonb")
    private List<TrackingEventJpaEntity> events;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        final LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
