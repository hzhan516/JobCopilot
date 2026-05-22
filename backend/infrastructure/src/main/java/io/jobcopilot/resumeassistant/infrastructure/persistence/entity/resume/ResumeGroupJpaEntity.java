package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历组 JPA 实体
 * Resume Group JPA Entity
 * <p>
 * 注意：
 * - 不使用 @Data 注解
 * - 使用 @EqualsAndHashCode(onlyExplicitlyIncluded = true) 仅基于 ID
 */
@Entity
@Table(name = "resume_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ResumeGroupJpaEntity {

    @Id
    @Column(columnDefinition = "UUID")
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private UUID userId;

    @Column(nullable = false, length = 255)
    @ToString.Include
    private String title;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
