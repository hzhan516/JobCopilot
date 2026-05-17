package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.matching;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 职位数据集 JPA 实体
 * Job dataset JPA entity
 */
@Entity
@Table(name = "job_dataset")
@Getter
@Setter
public class JobDatasetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(length = 200)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] requirements;

    @Column(length = 200)
    private String location;

    @Column(name = "experience_level", length = 50)
    private String experienceLevel;

    @Column(length = 50)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
