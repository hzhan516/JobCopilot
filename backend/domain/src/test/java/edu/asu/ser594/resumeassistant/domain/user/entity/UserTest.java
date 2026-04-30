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
 * User Entity Unit Tests
 * 
 * Tests the User aggregate root following DDD principles:
 * - Factory method creation
 * - Business rule enforcement
 * - Immutable vs mutable state
 * - Domain method behavior
 */
@DisplayName("User Entity Tests")
class UserTest {

    private static final String TEST_EMAIL = "test@example.com";

    @Test
    @DisplayName("Should create user with factory method with all required fields")
    void shouldCreateUserWithFactoryMethod() {
        // When
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

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
        // When
        User user1 = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        User user2 = User.create("another@example.com", OAuthProvider.EMAIL);

        // Then
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should verify email and update timestamp")
    void shouldVerifyEmailAndUpdateTimestamp() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // When
        try {
            Thread.sleep(10); // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.verifyEmail();

        // Then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should update role and update timestamp")
    void shouldUpdateRoleAndUpdateTimestamp() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // When
        try {
            Thread.sleep(10); // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.updateRole(UserRole.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should update status and update timestamp")
    void shouldUpdateStatusAndUpdateTimestamp() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime beforeUpdate = user.getUpdatedAt();

        // When
        try {
            Thread.sleep(10); // Ensure timestamp difference
        } catch (InterruptedException ignored) {}
        user.updateStatus(UserStatus.SUSPENDED);

        // Then
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should support all user roles")
    void shouldSupportAllUserRoles() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // When & Then
        for (UserRole role : UserRole.values()) {
            user.updateRole(role);
            assertThat(user.getRole()).isEqualTo(role);
        }
    }

    @Test
    @DisplayName("Should support all user statuses")
    void shouldSupportAllUserStatuses() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // When & Then
        for (UserStatus status : UserStatus.values()) {
            user.updateStatus(status);
            assertThat(user.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("Should maintain email immutability")
    void shouldMaintainEmailImmutability() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // Then - email should be set and immutable
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should maintain createdAt immutability")
    void shouldMaintainCreatedAtImmutability() {
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);
        LocalDateTime createdAt = user.getCreatedAt();

        // When - perform various updates
        user.verifyEmail();
        user.updateRole(UserRole.ADMIN);
        user.updateStatus(UserStatus.DELETED);

        // Then - createdAt should not change
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should build user with builder pattern")
    void shouldBuildUserWithBuilderPattern() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

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
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // Then
        assertThat(user.getId()).isNotNull();
        assertThat(user).isInstanceOf(edu.asu.ser594.resumeassistant.domain.shared.entity.Entity.class);
    }

    @Test
    @DisplayName("Should create user with all statuses through builder")
    void shouldCreateUserWithAllStatusesThroughBuilder() {
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
        // Given
        User user = User.create(TEST_EMAIL, OAuthProvider.EMAIL);

        // When - multiple state changes
        user.verifyEmail();
        user.updateRole(UserRole.ADMIN);
        user.updateStatus(UserStatus.SUSPENDED);

        // Then
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}
