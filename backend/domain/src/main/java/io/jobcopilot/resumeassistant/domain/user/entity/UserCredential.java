package io.jobcopilot.resumeassistant.domain.user.entity;

import io.jobcopilot.resumeassistant.domain.shared.entity.Entity;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户凭证实体
 * User credential entity
 * <p>
 * 不变性：凭证创建后不可变，密码修改创建新凭证记录
 * Immutability: credentials are immutable after creation, password changes create new credential records
 */
@Getter
@Builder
public class UserCredential implements Entity<UUID> {

    private final UUID id;
    private final UUID userId;
    private final CredentialType credentialType;
    private final String credentialValue;
    private final LocalDateTime lastChangedAt;
    private final LocalDateTime createdAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     */
    UserCredential(UUID id, UUID userId, CredentialType credentialType,
                   String credentialValue, LocalDateTime lastChangedAt, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.credentialType = credentialType;
        this.credentialValue = credentialValue;
        this.lastChangedAt = lastChangedAt;
        this.createdAt = createdAt;
    }

    /**
     * 工厂方法：创建密码凭证
     * Factory method: create password credential
     */
    public static UserCredential createPassword(UUID userId, String hashedPassword) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return UserCredential.builder()
                .id(id)
                .userId(userId)
                .credentialType(CredentialType.PASSWORD)
                .credentialValue(hashedPassword)
                .lastChangedAt(now)
                .createdAt(now)
                .build();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
