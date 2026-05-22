package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume;

import io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for a resume version snapshot.
 * <p>Design notes:</p>
 * <ul>
 *   <li>Avoids @Data to prevent accidental toString exposure of large text/JSON fields</li>
 *   <li>@EqualsAndHashCode scoped to ID only for stable collection behavior</li>
 *   <li>parsed_content mapped as JSONB via @JdbcTypeCode(SqlTypes.JSON)</li>
 * </ul>
 * 简历版本快照的 JPA 实体
 * <p>设计注意：</p>
 * <ul>
 *   <li>不使用 @Data，防止 toString 意外输出大文本/JSON 字段</li>
 *   <li>@EqualsAndHashCode 仅基于 ID，保证集合行为稳定</li>
 *   <li>parsed_content 通过 @JdbcTypeCode(SqlTypes.JSON) 映射为 PostgreSQL JSONB</li>
 * </ul>
 */
@Entity
@Table(name = "resume_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ResumeVersionJpaEntity {

    @Id
    @Column(columnDefinition = "UUID")
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "group_id", nullable = false)
    @ToString.Include
    private UUID groupId;

    @Column(name = "version_type", nullable = false, length = 20)
    @ToString.Include
    private String versionType;

    @Column(name = "original_file_name", length = 255)
    @ToString.Include
    private String originalFileName;

    @Column(name = "stored_file_name", length = 255)
    private String storedFileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;

    @Column(name = "storage_provider", length = 50)
    private String storageProvider;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "parsed_content")
    @JdbcTypeCode(SqlTypes.JSON)
    private String parsedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false, length = 20)
    private ParseStatus parseStatus;

    @Column(name = "parse_error_message", columnDefinition = "TEXT")
    private String parseErrorMessage;

    @Column(nullable = false, length = 20)
    @ToString.Include
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
