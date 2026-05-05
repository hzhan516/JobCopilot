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
 * - 所有字段均为 final，实体完全不可变
 * - 使用 @Builder(toBuilder = true) 支持构建器模式和基于现有对象的不可变更新
 * - 属性修改通过领域方法返回新实例，确保业务规则校验和副作用可追踪
 * <p>
 * Design notes:
 * - All fields are final; the entity is fully immutable.
 * - Uses @Builder(toBuilder = true) to support builder pattern and immutable updates.
 * - Attribute modifications return new instances via domain methods,
 *   ensuring business rule validation and traceable side effects.
 */
@Getter
@Builder(toBuilder = true)
public class UserProfile implements Entity<UUID> {

    private final UUID id;
    private final UUID userId;
    private final LocalDateTime createdAt;
    private final String fullName;
    private final String avatarUrl;
    private final String phone;
    private final String targetPosition;
    private final String preferredLocation;
    private final LocalDateTime updatedAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     */
    UserProfile(UUID id, UUID userId, LocalDateTime createdAt, String fullName, String avatarUrl,
                String phone, String targetPosition, String preferredLocation,
                LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.targetPosition = targetPosition;
        this.preferredLocation = preferredLocation;
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
     * Update avatar.
     * <p>
     * 返回一个新的不可变实例，原实例保持不变。
     * Returns a new immutable instance; the original remains unchanged.
     *
     * @param avatarUrl 新头像 URL / New avatar URL
     * @return 更新后的用户资料 / Updated user profile
     */
    public UserProfile updateAvatar(String avatarUrl) {
        return this.toBuilder()
                .avatarUrl(avatarUrl)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 更新个人资料
     * Update profile.
     * <p>
     * 返回一个新的不可变实例，原实例保持不变。
     * 仅非空参数会覆盖现有值。
     * Returns a new immutable instance; the original remains unchanged.
     * Only non-null parameters override existing values.
     *
     * @param fullName         全名 / Full name
     * @param phone            电话 / Phone
     * @param targetPosition   目标职位 / Target position
     * @param preferredLocation 偏好地点 / Preferred location
     * @return 更新后的用户资料 / Updated user profile
     */
    public UserProfile updateProfile(String fullName, String phone, String targetPosition, String preferredLocation) {
        UserProfileBuilder builder = this.toBuilder()
                .updatedAt(LocalDateTime.now());

        if (fullName != null) {
            builder.fullName(fullName);
        }
        if (phone != null) {
            builder.phone(phone);
        }
        if (targetPosition != null) {
            builder.targetPosition(targetPosition);
        }
        if (preferredLocation != null) {
            builder.preferredLocation(preferredLocation);
        }

        return builder.build();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
