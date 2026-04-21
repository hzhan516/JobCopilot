package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户凭证实体
 * User credential entity
 *
 * 不变性：凭证创建后不可变，密码修改创建新凭证记录
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
