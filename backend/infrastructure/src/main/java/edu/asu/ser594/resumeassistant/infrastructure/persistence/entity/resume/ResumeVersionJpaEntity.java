package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume;

import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历版本 JPA 实体
 * Resume Version JPA Entity
 * <p>
 * 注意：
 * - 不使用 @Data 注解
 * - 使用 @EqualsAndHashCode(onlyExplicitlyIncluded = true) 仅基于 ID
 * - content 和 parsedContent 为大文本/JSON 字段，避免在 toString 中输出
 * - parsed_content 使用 @JdbcTypeCode(SqlTypes.JSON) 映射 PostgreSQL JSONB 类型
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
    // 注意：不在 toString 中包含大文本内容
    private String content;

    @Column(name = "parsed_content")
    @JdbcTypeCode(SqlTypes.JSON)
    // 注意：不在 toString 中包含 JSON 内容
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
