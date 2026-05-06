package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.valueobject.VectorStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 职位向量 JPA 实体
 * Job vector JPA entity
 */
@Entity
@Table(name = "job_vectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobVectorJpaEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    /**
     * 向量数据，使用 float[] 映射 pgvector
     * Vector data, using float[] to map to pgvector
     */
    /**
     * 嵌入向量，使用 Hibernate 6.2+ 的 VECTOR 类型映射 pgvector。
     * Embedding vector mapped to pgvector via Hibernate 6.2+ VECTOR type.
     * <p>
     * 注意：pgvector 列维度由数据库 schema（init.sql / Flyway）管理，
     * 不在此处硬编码，以便通过 LLM_EMBEDDING_MODEL_DIMENSION 动态调整。
     * The column dimension is managed by DB schema (init.sql / Flyway),
     * not hard-coded here, to allow dynamic adjustment via LLM_EMBEDDING_MODEL_DIMENSION.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(name = "embedding")
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VectorStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private List<String> requirements;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "source_file", length = 255)
    private String sourceFile;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}