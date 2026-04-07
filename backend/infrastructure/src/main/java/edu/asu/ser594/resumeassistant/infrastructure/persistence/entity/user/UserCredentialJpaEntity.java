package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user;

import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// User credential JPA entity
@Entity
@Table(name = "user_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserCredentialJpaEntity {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credential_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CredentialType credentialType;

    @Column(name = "credential_value", nullable = false)
    private String credentialValue;

    @Column(name = "last_changed_at")
    private LocalDateTime lastChangedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}