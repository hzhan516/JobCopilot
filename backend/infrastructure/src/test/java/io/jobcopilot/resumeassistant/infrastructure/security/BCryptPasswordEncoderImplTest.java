package io.jobcopilot.resumeassistant.infrastructure.security;

import io.jobcopilot.resumeassistant.domain.user.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BCryptPasswordEncoderImpl 单元测试
 * BCryptPasswordEncoderImpl Unit Tests
 * <p>
 * 测试 BCrypt 密码编码器实现：
 * Tests the BCrypt password encoder implementation:
 * - 密码编码
 * - Password encoding
 * - 密码匹配
 * - Password matching
 * - 不同密码产生不同哈希
 * - Different passwords produce different hashes
 */
@DisplayName("BCrypt Password Encoder Implementation Tests")
class BCryptPasswordEncoderImplTest {

    private static final String RAW_PASSWORD = "mySecretPassword123";
    private static final String DIFFERENT_PASSWORD = "differentPassword456";

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoderImpl();
    }

    // ==================== 编码测试 ====================
    // ==================== Encoding Tests ====================

    @Test
    @DisplayName("Should encode password to non-empty string")
    void shouldEncodePasswordToNonEmptyString() {
        // 当
        // When
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // 然后
        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should produce different hashes for same password")
    void shouldProduceDifferentHashesForSamePassword() {
        // 当
        // When
        String encoded1 = passwordEncoder.encode(RAW_PASSWORD);
        String encoded2 = passwordEncoder.encode(RAW_PASSWORD);

        // 然后
        // Then
        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    @DisplayName("Should handle empty password")
    void shouldHandleEmptyPassword() {
        // 当
        // When
        String encoded = passwordEncoder.encode("");

        // 然后
        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should handle long password")
    void shouldHandleLongPassword() {
        // 给定
        // Given
        String longPassword = "a".repeat(100);

        // 当
        // When
        String encoded = passwordEncoder.encode(longPassword);

        // 然后
        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should handle password with special characters")
    void shouldHandlePasswordWithSpecialCharacters() {
        // 给定
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        // 当
        // When
        String encoded = passwordEncoder.encode(specialPassword);

        // 然后
        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
        assertThat(passwordEncoder.matches(specialPassword, encoded)).isTrue();
    }

    // ==================== 匹配测试 ====================
    // ==================== Matching Tests ====================

    @Test
    @DisplayName("Should match correct password")
    void shouldMatchCorrectPassword() {
        // 给定
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // 当
        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, encoded);

        // 然后
        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match incorrect password")
    void shouldNotMatchIncorrectPassword() {
        // 给定
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // 当
        // When
        boolean matches = passwordEncoder.matches(DIFFERENT_PASSWORD, encoded);

        // 然后
        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match encoded password with itself")
    void shouldMatchEncodedPasswordWithItself() {
        // 给定
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // 当
        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, encoded);

        // 然后
        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match with null password")
    void shouldNotMatchWithNullPassword() {
        // 给定
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // 当
        // When
        boolean matches = passwordEncoder.matches(null, encoded);

        // 然后
        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should not match with null encoded password")
    void shouldNotMatchWithNullEncodedPassword() {
        // 当
        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, null);

        // 然后
        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should not match when both are null")
    void shouldNotMatchWhenBothAreNull() {
        // 当
        // When
        boolean matches = passwordEncoder.matches(null, null);

        // 然后
        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should handle case sensitivity")
    void shouldHandleCaseSensitivity() {
        // 给定
        // Given
        String lowerCasePassword = "password";
        String upperCasePassword = "PASSWORD";
        String encoded = passwordEncoder.encode(lowerCasePassword);

        // 当
        // When
        boolean lowerMatches = passwordEncoder.matches(lowerCasePassword, encoded);
        boolean upperMatches = passwordEncoder.matches(upperCasePassword, encoded);

        // 然后
        // Then
        assertThat(lowerMatches).isTrue();
        assertThat(upperMatches).isFalse();
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void shouldHandleUnicodeCharacters() {
        // 给定
        // Given
        String unicodePassword = "密码パスワード🔐";

        // 当
        // When
        String encoded = passwordEncoder.encode(unicodePassword);
        boolean matches = passwordEncoder.matches(unicodePassword, encoded);

        // 然后
        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
        assertThat(matches).isTrue();
    }
}
