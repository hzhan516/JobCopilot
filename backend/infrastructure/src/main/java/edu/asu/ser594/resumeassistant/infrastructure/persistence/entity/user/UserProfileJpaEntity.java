package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户资料 JPA 实体
 * User profile JPA entity
 * <p>
 * 注意：
 * - 不使用 @Data 注解
 * - 使用 @EqualsAndHashCode(onlyExplicitlyIncluded = true) 仅基于 ID
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserProfileJpaEntity {

    @Id
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    @ToString.Include
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "target_position")
    private String targetPosition;

    @Column(name = "preferred_location")
    private String preferredLocation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
