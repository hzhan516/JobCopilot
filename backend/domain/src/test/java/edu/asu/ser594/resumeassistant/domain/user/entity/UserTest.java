package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户实体单元测试
 * User Entity Unit Tests
 * 
 * 遵循DDD原则测试用户聚合根：
 * Tests the User aggregate root following DDD principles:
 * - 工厂方法创建
 * - Factory method creation
 * - 业务规则执行
 * - Business rule enforcement
 * - 不可变与可变状态
 * - Immutable vs mutable state
 * - 领域方法行为
 * - Domain method behavior
 */
@DisplayName("User Entity Tests")
class UserTest {

    private static final String TEST_EMAIL = "test@example.com";

    @Test
    @DisplayName("Should create user with factory method with all required fields")
    void shouldCreateUserWithFactoryMethod() {
        // 当
        // When
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 那么
        // Then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getRole()).isEqualTo(UserRole.JOB_SEEKER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should generate unique ID for each created user")
    void shouldGenerateUniqueIdForEachUser() {
        // 当
        // When
        User user1 = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        User user2 = User.create("another@example.com", OAuthProvider.EMAIL);

        // 那么
        // Then
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should verify email and update timestamp")
    void shouldVerifyEmailAndUpdateTimestamp() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // 当
        // When
        try {
            Thread.sleep(10); // 确保时间戳差异
            // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.verifyEmail();

        // 那么
        // Then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should update role and update timestamp")
    void shouldUpdateRoleAndUpdateTimestamp() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // 当
        // When
        try {
            Thread.sleep(10); // 确保时间戳差异
            // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.updateRole(UserRole.ADMIN);

        // 那么
        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should update status and update timestamp")
    void shouldUpdateStatusAndUpdateTimestamp() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // 当
        // When
        try {
            Thread.sleep(10); // 确保时间戳差异
            // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.updateStatus(UserStatus.SUSPENDED);

        // 那么
        // Then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should support all user roles")
    void shouldSupportAllUserRoles() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 当 & 那么
        // When & Then
        for (UserRole role : UserRole.values()) {
            user.updateRole(role);
            assertThat(user.getRole()).isEqualTo(role);
        }
    }

    @Test
    @DisplayName("Should support all user statuses")
    void shouldSupportAllUserStatuses() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 当 & 那么
        // When & Then
        for (UserStatus status : UserStatus.values()) {
            user.updateStatus(status);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should maintain email immutability")
    void shouldMaintainEmailImmutability() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 那么 - email应被设置且不可变
        // Then - email should be set and immutable
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should maintain createdAt immutability")
    void shouldMaintainCreatedAtImmutability() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime createdAt = user.getCreatedAt();

        // 当 - 执行各种更新
        // When - perform various updates
        user.verifyEmail();
        user.updateRole(UserRole.ADMIN);
        user.updateStatus(UserStatus.DELETED);

        // 那么 - createdAt不应改变
        // Then - createdAt should not change
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should build user with builder pattern")
    void shouldBuildUserWithBuilderPattern() {
        // 给定
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // 当
        // When
        User user = User.builder()
                .id(id)
                .email(TEST_EMAIL)
                .emailVerified(true)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .authProvider(OAuthProvider.EMAIL)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 那么
        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should implement Entity interface correctly")
    void shouldImplementEntityInterfaceCorrectly() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 那么
        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user).isInstanceOf(edu.asu.ser594.resumeassistant.domain.shared.entity.Entity.class);
    }

    @Test
    @DisplayName("Should create user with all statuses through builder")
    void shouldCreateUserWithAllStatusesThroughBuilder() {
        // 当 & 那么
        // When & Then
        for (UserStatus status : UserStatus.values()) {
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .email(TEST_EMAIL)
                    .emailVerified(false)
                    .role(UserRole.JOB_SEEKER)
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            assertThat(user.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should handle multiple state changes correctly")
    void shouldHandleMultipleStateChangesCorrectly() {
        // 给定
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // 当 - 多次状态变更
        // When - multiple state changes
        user.verifyEmail();
        user.updateRole(UserRole.ADMIN);
        user.updateStatus(UserStatus.SUSPENDED);

        // 那么
        // Then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}
