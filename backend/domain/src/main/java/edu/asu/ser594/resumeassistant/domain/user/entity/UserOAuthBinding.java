package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth 绑定实体
 * OAuth binding entity
 *
 * 记录用户与第三方认证提供商的绑定关系
 * Records the binding relationship between user and third-party auth provider
 */
@Getter
@Builder
public class UserOAuthBinding implements Entity<UUID> {

    private final UUID id;
    private final UUID userId;
    private final OAuthProvider provider;
    private final String providerUserId;
    private final String email;
    private String displayName;
    private String avatarUrl;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     */
    UserOAuthBinding(UUID id, UUID userId, OAuthProvider provider, String providerUserId,
                     String email, String displayName, String avatarUrl,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 工厂方法：创建新的 OAuth 绑定
     * Factory method: create new OAuth binding
     */
    public static UserOAuthBinding create(UUID userId, OAuthProvider provider,
                                          String providerUserId, String email,
                                          String displayName, String avatarUrl) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return UserOAuthBinding.builder()
                .id(id)
                .userId(userId)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 更新显示信息
     * Update display info
     */
    public void updateDisplayInfo(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
