package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user;

import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for user authentication credentials.
 * <p>Design notes:</p>
 * <ul>
 *   <li>Avoids @Data to prevent accidental exposure of sensitive fields in logs</li>
 *   <li>credentialValue is explicitly excluded from @ToString</li>
 * </ul>
 * 用户认证凭证的 JPA 实体
 * <p>设计注意：</p>
 * <ul>
 *   <li>不使用 @Data，防止敏感字段在日志中意外暴露</li>
 *   <li>credentialValue 显式排除在 @ToString 之外</li>
 * </ul>
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
    private String credentialValue;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
