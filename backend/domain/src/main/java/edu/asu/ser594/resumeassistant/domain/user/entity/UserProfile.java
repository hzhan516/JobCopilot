package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户资料实体
 * User profile entity
 * <p>
 * 设计说明：
 * - 使用 @Getter 暴露字段访问
 * - 使用 @Builder(toBuilder = true) 支持构建器模式和基于现有对象的更新
 * - 可变字段通过领域方法修改，确保业务规则校验
 */
@Getter
@Builder(toBuilder = true)
public class UserProfile implements Entity<UUID> {

    private final UUID id;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private String targetPosition;
    private String preferredLocation;
    private LocalDateTime updatedAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     */
    UserProfile(UUID id, UUID userId, String fullName, String avatarUrl,
                String phone, String targetPosition, String preferredLocation,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.targetPosition = targetPosition;
        this.preferredLocation = preferredLocation;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 工厂方法：创建新用户资料
     * Factory method: create new user profile
     */
    public static UserProfile create(UUID userId) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return UserProfile.builder()
                .id(id)
                .userId(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 更新头像
     * Update avatar
     */
    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新个人资料
     * Update profile
     */
    public void updateProfile(String fullName, String phone, String targetPosition, String preferredLocation) {
        if (fullName != null) {
            this.fullName = fullName;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (targetPosition != null) {
            this.targetPosition = targetPosition;
        }
        if (preferredLocation != null) {
            this.preferredLocation = preferredLocation;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
