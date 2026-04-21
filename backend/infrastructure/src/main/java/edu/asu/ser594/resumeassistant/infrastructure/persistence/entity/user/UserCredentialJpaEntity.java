package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user;

import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户凭证 JPA 实体
 * User credential JPA entity
 *
 * 注意：
 * - 不使用 @Data 注解，避免 toString 暴露敏感信息
 * - @ToString 排除 credentialValue 字段
 */
@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserCredentialJpaEntity {

    @Id
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private UUID userId;

    @Column(name = "credential_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @ToString.Include
    private CredentialType credentialType;

    @Column(name = "credential_value", nullable = false)
    // 注意：不在 toString 中包含敏感信息
    private String credentialValue;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
