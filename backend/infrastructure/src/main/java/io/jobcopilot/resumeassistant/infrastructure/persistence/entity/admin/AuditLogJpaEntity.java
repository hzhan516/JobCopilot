package io.jobcopilot.resumeassistant.infrastructure.persistence.entity.admin;

import io.jobcopilot.resumeassistant.domain.admin.entity.AuditLog;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 255)
    private String targetId;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public AuditLog toDomain() {
        return AuditLog.builder()
                .id(id).adminUserId(adminUserId).action(action)
                .targetType(targetType).targetId(targetId)
                .details(details).ipAddress(ipAddress).createdAt(createdAt)
                .build();
    }

    public static AuditLogJpaEntity fromDomain(AuditLog log) {
        var e = new AuditLogJpaEntity();
        e.setId(log.getId());
        e.setAdminUserId(log.getAdminUserId());
        e.setAction(log.getAction());
        e.setTargetType(log.getTargetType());
        e.setTargetId(log.getTargetId());
        e.setDetails(log.getDetails());
        e.setIpAddress(log.getIpAddress());
        e.setCreatedAt(log.getCreatedAt());
        return e;
    }
}
