package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户聚合根
 * User aggregate root
 * <p>
 * 设计说明：
 * - 使用 @Getter 暴露字段访问
 * - 使用 @Builder 支持构建器模式
 * - 使用全参构造函数，但保持私有通过工厂方法创建
 * - 可变字段通过领域方法修改，确保业务规则校验
 */
@Getter
@Builder
public class User extends AggregateRoot<UUID> {

    private final UUID id;
    private final String email;
    private final LocalDateTime createdAt;
    private boolean emailVerified;
    private UserRole role;
    private UserStatus status;
    private OAuthProvider authProvider;
    private LocalDateTime updatedAt;

    /**
     * 全参构造函数 - 由 Lombok @Builder 使用
     * All-args constructor used by Lombok @Builder
     * 注意：@Builder 生成的代码可以访问包级别或私有的全参构造器
     * Note: @Builder generated code can access package-level or private all-args constructor
     */
    User(UUID id, String email, LocalDateTime createdAt, boolean emailVerified, UserRole role,
         UserStatus status, OAuthProvider authProvider, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
        this.emailVerified = emailVerified;
        this.role = role;
        this.status = status;
        this.authProvider = authProvider;
        this.updatedAt = updatedAt;
    }

    /**
     * 工厂方法：创建新用户
     * Factory method: create new user
     *
     * @param email        邮箱地址
     * @param authProvider 认证提供者
     */
    public static User create(String email, OAuthProvider authProvider) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        boolean isEmailVerified = authProvider == OAuthProvider.GOOGLE;
        return User.builder()
                .id(id)
                .email(email)
                .emailVerified(isEmailVerified)
                .role(UserRole.JOB_SEEKER)
                .status(UserStatus.ACTIVE)
                .authProvider(authProvider)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 验证邮箱
     * Verify email
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新用户角色
     * Update user role
     */
    public void updateRole(UserRole newRole) {
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新用户状态
     * Update user status
     */
    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}
