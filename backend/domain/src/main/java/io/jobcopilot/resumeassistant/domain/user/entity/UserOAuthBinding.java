package io.jobcopilot.resumeassistant.domain.user.entity;

import io.jobcopilot.resumeassistant.domain.shared.entity.Entity;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth 绑定实体
 * OAuth binding entity
 * <p>
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
    private final LocalDateTime createdAt;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime updatedAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     */
    UserOAuthBinding(UUID id, UUID userId, OAuthProvider provider, String providerUserId,
                     String email, LocalDateTime createdAt, String displayName, String avatarUrl,
                     LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = createdAt;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
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
