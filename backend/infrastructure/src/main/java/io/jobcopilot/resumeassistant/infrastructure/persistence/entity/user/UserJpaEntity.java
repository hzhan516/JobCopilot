package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user;

import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户 JPA 实体
 * User JPA entity
 * <p>
 * 注意：
 * - 不使用 @Data 注解，因为它会生成 toString/equals/hashCode 可能影响懒加载
 * - 使用 @Getter/@Setter 显式控制访问器
 * - 使用 @EqualsAndHashCode(onlyExplicitlyIncluded = true) 仅基于 ID
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserJpaEntity {

    @Id
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "auth_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private OAuthProvider authProvider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
