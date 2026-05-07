package edu.asu.ser594.resumeassistant.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户资料实体单元测试
 * UserProfile Entity Unit Tests
 * <p>
 * 遵循DDD原则测试用户资料实体：
 * Tests the UserProfile entity following DDD principles:
 * - 工厂方法创建
 * - Factory method creation
 * - 不可变更新（toBuilder）
 * - Immutable updates via toBuilder
 * - 原实例在更新后保持不变
 * - Original instance remains unchanged after update
 */
@DisplayName("UserProfile Entity Tests")
class UserProfileTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Should create profile with factory method")
    void shouldCreateProfileWithFactoryMethod() {
        // 当
        // When
        UserProfile profile = UserProfile.create(TEST_USER_ID);

        // 那么
        // Then
        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(profile.getFullName()).isNull();
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getPhone()).isNull();
        assertThat(profile.getTargetPosition()).isNull();
        assertThat(profile.getPreferredLocation()).isNull();
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return new instance when updating avatar and leave original unchanged")
    void shouldReturnNewInstanceWhenUpdatingAvatarAndLeaveOriginalUnchanged() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        LocalDateTime beforeUpdate = profile.getUpdatedAt();
        String avatarUrl = "https://example.com/avatar.jpg";

        // 当
        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
        UserProfile updated = profile.updateAvatar(avatarUrl);

        // 那么
        // Then
        assertThat(updated).isNotNull();
        assertThat(updated).isNotSameAs(profile);
        assertThat(updated.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

        // 原实例应保持不变 / Original instance should remain unchanged
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getUpdatedAt()).isEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should return new instance when updating profile with all fields")
    void shouldReturnNewInstanceWhenUpdatingProfileWithAllFields() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        String fullName = "John Doe";
        String phone = "+1-555-123-4567";
        String targetPosition = "Senior Software Engineer";
        String preferredLocation = "San Francisco, CA";

        // 当
        // When
        UserProfile updated = profile.updateProfile(fullName, phone, targetPosition, preferredLocation);

        // 那么
        // Then
        assertThat(updated).isNotNull();
        assertThat(updated).isNotSameAs(profile);
        assertThat(updated.getFullName()).isEqualTo(fullName);
        assertThat(updated.getPhone()).isEqualTo(phone);
        assertThat(updated.getTargetPosition()).isEqualTo(targetPosition);
        assertThat(updated.getPreferredLocation()).isEqualTo(preferredLocation);

        // 原实例应保持不变 / Original instance should remain unchanged
        assertThat(profile.getFullName()).isNull();
        assertThat(profile.getPhone()).isNull();
        assertThat(profile.getTargetPosition()).isNull();
        assertThat(profile.getPreferredLocation()).isNull();
    }

    @Test
    @DisplayName("Should update profile with partial fields and leave original unchanged")
    void shouldUpdateProfileWithPartialFieldsAndLeaveOriginalUnchanged() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        String fullName = "Jane Doe";

        // 当
        // When
        UserProfile updated = profile.updateProfile(fullName, null, null, null);

        // 那么
        // Then
        assertThat(updated).isNotSameAs(profile);
        assertThat(updated.getFullName()).isEqualTo(fullName);
        assertThat(updated.getPhone()).isNull();
        assertThat(updated.getTargetPosition()).isNull();
        assertThat(updated.getPreferredLocation()).isNull();

        // 原实例应保持不变 / Original instance should remain unchanged
        assertThat(profile.getFullName()).isNull();
    }

    @Test
    @DisplayName("Should not update fields when null values provided")
    void shouldNotUpdateFieldsWhenNullValuesProvided() {
        // 给定
        // Given
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .fullName("Original Name")
                .phone("123-456")
                .targetPosition("Developer")
                .preferredLocation("NYC")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 当
        // When
        UserProfile updated = profile.updateProfile(null, null, null, null);

        // 那么
        // Then
        assertThat(updated).isNotSameAs(profile);
        assertThat(updated.getFullName()).isEqualTo("Original Name");
        assertThat(updated.getPhone()).isEqualTo("123-456");
        assertThat(updated.getTargetPosition()).isEqualTo("Developer");
        assertThat(updated.getPreferredLocation()).isEqualTo("NYC");
    }

    @Test
    @DisplayName("Should build profile with builder pattern")
    void shouldBuildProfileWithBuilderPattern() {
        // 给定
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // 当
        // When
        UserProfile profile = UserProfile.builder()
                .id(id)
                .userId(TEST_USER_ID)
                .fullName("Test User")
                .avatarUrl("https://example.com/avatar.png")
                .phone("+1-555-999-8888")
                .targetPosition("Software Architect")
                .preferredLocation("Remote")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 那么
        // Then
        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(profile.getFullName()).isEqualTo("Test User");
        assertThat(profile.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(profile.getPhone()).isEqualTo("+1-555-999-8888");
        assertThat(profile.getTargetPosition()).isEqualTo("Software Architect");
        assertThat(profile.getPreferredLocation()).isEqualTo("Remote");
    }

    @Test
    @DisplayName("Should create modified copy using toBuilder")
    void shouldCreateModifiedCopyUsingToBuilder() {
        // 给定
        // Given
        UserProfile original = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .fullName("Original Name")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 当
        // When
        UserProfile modified = original.toBuilder()
                .fullName("Modified Name")
                .build();

        // 那么
        // Then
        assertThat(modified.getId()).isEqualTo(original.getId());
        assertThat(modified.getUserId()).isEqualTo(original.getUserId());
        assertThat(modified.getFullName()).isEqualTo("Modified Name");
        assertThat(original.getFullName()).isEqualTo("Original Name");
    }

    @Test
    @DisplayName("Should maintain userId immutability")
    void shouldMaintainUserIdImmutability() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);

        // 那么
        // Then
        assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should maintain createdAt immutability across updates")
    void shouldMaintainCreatedAtImmutabilityAcrossUpdates() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        LocalDateTime createdAt = profile.getCreatedAt();

        // 当 - 执行更新
        // When - perform updates
        UserProfile afterAvatar = profile.updateAvatar("url");
        UserProfile afterProfile = afterAvatar.updateProfile("Name", "Phone", "Position", "Location");

        // 那么
        // Then
        assertThat(profile.getCreatedAt()).isEqualTo(createdAt);
        assertThat(afterAvatar.getCreatedAt()).isEqualTo(createdAt);
        assertThat(afterProfile.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Should update timestamp on every modification")
    void shouldUpdateTimestampOnEveryModification() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        LocalDateTime initialTimestamp = profile.getUpdatedAt();

        // 当
        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
        UserProfile afterAvatar = profile.updateAvatar("url1");
        LocalDateTime afterAvatarUpdate = afterAvatar.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {
        }
        UserProfile afterProfile = afterAvatar.updateProfile("Name", null, null, null);
        LocalDateTime afterProfileUpdate = afterProfile.getUpdatedAt();

        // 那么
        // Then
        assertThat(afterAvatarUpdate).isAfter(initialTimestamp);
        assertThat(afterProfileUpdate).isAfter(afterAvatarUpdate);
    }
}
