package edu.asu.ser594.resumeassistant.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户资料实体单元测试
 * UserProfile Entity Unit Tests
 * 
 * 遵循DDD原则测试用户资料实体：
 * Tests the UserProfile entity following DDD principles:
 * - 工厂方法创建
 * - Factory method creation
 * - 资料更新
 * - Profile updates
 * - 带toBuilder的构建器模式
 * - Builder pattern with toBuilder
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
    @DisplayName("Should update avatar and timestamp")
    void shouldUpdateAvatarAndTimestamp() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        LocalDateTime beforeUpdate = profile.getUpdatedAt();
        String avatarUrl = "https://example.com/avatar.jpg";

        // 当
        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        profile.updateAvatar(avatarUrl);

        // 那么
        // Then
        assertThat(profile.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(profile.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("Should update profile with all fields")
    void shouldUpdateProfileWithAllFields() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        String fullName = "John Doe";
        String phone = "+1-555-123-4567";
        String targetPosition = "Senior Software Engineer";
        String preferredLocation = "San Francisco, CA";

        // 当
        // When
        profile.updateProfile(fullName, phone, targetPosition, preferredLocation);

        // 那么
        // Then
        assertThat(profile.getFullName()).isEqualTo(fullName);
        assertThat(profile.getPhone()).isEqualTo(phone);
        assertThat(profile.getTargetPosition()).isEqualTo(targetPosition);
        assertThat(profile.getPreferredLocation()).isEqualTo(preferredLocation);
    }

    @Test
    @DisplayName("Should update profile with partial fields")
    void shouldUpdateProfileWithPartialFields() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        String fullName = "Jane Doe";

        // 当
        // When
        profile.updateProfile(fullName, null, null, null);

        // 那么
        // Then
        assertThat(profile.getFullName()).isEqualTo(fullName);
        assertThat(profile.getPhone()).isNull();
        assertThat(profile.getTargetPosition()).isNull();
        assertThat(profile.getPreferredLocation()).isNull();
    }

    @Test
    @DisplayName("Should not update fields when null values provided")
    void shouldNotUpdateFieldsWhenNullValuesProvided() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        profile.updateProfile("Original Name", "123-456", "Developer", "NYC");

        // 当
        // When
        profile.updateProfile(null, null, null, null);

        // 那么
        // Then
        assertThat(profile.getFullName()).isEqualTo("Original Name");
        assertThat(profile.getPhone()).isEqualTo("123-456");
        assertThat(profile.getTargetPosition()).isEqualTo("Developer");
        assertThat(profile.getPreferredLocation()).isEqualTo("NYC");
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
    @DisplayName("Should maintain createdAt immutability")
    void shouldMaintainCreatedAtImmutability() {
        // 给定
        // Given
        UserProfile profile = UserProfile.create(TEST_USER_ID);
        LocalDateTime createdAt = profile.getCreatedAt();

        // 当 - 执行更新
        // When - perform updates
        profile.updateAvatar("url");
        profile.updateProfile("Name", "Phone", "Position", "Location");

        // 那么
        // Then
        assertThat(profile.getCreatedAt()).isEqualTo(createdAt);
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
        } catch (InterruptedException ignored) {}
        profile.updateAvatar("url1");
        LocalDateTime afterAvatarUpdate = profile.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        profile.updateProfile("Name", null, null, null);
        LocalDateTime afterProfileUpdate = profile.getUpdatedAt();

        // 那么
        // Then
        assertThat(afterAvatarUpdate).isAfter(initialTimestamp);
        assertThat(afterProfileUpdate).isAfter(afterAvatarUpdate);
    }
}
