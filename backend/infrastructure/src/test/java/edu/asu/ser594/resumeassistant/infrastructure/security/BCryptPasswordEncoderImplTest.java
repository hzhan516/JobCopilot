package edu.asu.ser594.resumeassistant.infrastructure.security;

import edu.asu.ser594.resumeassistant.domain.user.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BCryptPasswordEncoderImpl Unit Tests
 * 
 * Tests the BCrypt password encoder implementation:
 * - Password encoding
 * - Password matching
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

    // ==================== Encoding Tests ====================

    @Test
    @DisplayName("Should encode password to non-empty string")
    void shouldEncodePasswordToNonEmptyString() {
        // When
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should produce different hashes for same password")
    void shouldProduceDifferentHashesForSamePassword() {
        // When
        String encoded1 = passwordEncoder.encode(RAW_PASSWORD);
        String encoded2 = passwordEncoder.encode(RAW_PASSWORD);

        // Then
        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    @DisplayName("Should handle empty password")
    void shouldHandleEmptyPassword() {
        // When
        String encoded = passwordEncoder.encode("");

        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should handle long password")
    void shouldHandleLongPassword() {
        // Given
        String longPassword = "a".repeat(100);

        // When
        String encoded = passwordEncoder.encode(longPassword);

        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should handle password with special characters")
    void shouldHandlePasswordWithSpecialCharacters() {
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

        // When
        String encoded = passwordEncoder.encode(specialPassword);

        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
        assertThat(passwordEncoder.matches(specialPassword, encoded)).isTrue();
    }

    // ==================== Matching Tests ====================

    @Test
    @DisplayName("Should match correct password")
    void shouldMatchCorrectPassword() {
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, encoded);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match incorrect password")
    void shouldNotMatchIncorrectPassword() {
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // When
        boolean matches = passwordEncoder.matches(DIFFERENT_PASSWORD, encoded);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should match encoded password with itself")
    void shouldMatchEncodedPasswordWithItself() {
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, encoded);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should not match with null password")
    void shouldNotMatchWithNullPassword() {
        // Given
        String encoded = passwordEncoder.encode(RAW_PASSWORD);

        // When
        boolean matches = passwordEncoder.matches(null, encoded);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should not match with null encoded password")
    void shouldNotMatchWithNullEncodedPassword() {
        // When
        boolean matches = passwordEncoder.matches(RAW_PASSWORD, null);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should not match when both are null")
    void shouldNotMatchWhenBothAreNull() {
        // When
        boolean matches = passwordEncoder.matches(null, null);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should handle case sensitivity")
    void shouldHandleCaseSensitivity() {
        // Given
        String lowerCasePassword = "password";
        String upperCasePassword = "PASSWORD";
        String encoded = passwordEncoder.encode(lowerCasePassword);

        // When
        boolean lowerMatches = passwordEncoder.matches(lowerCasePassword, encoded);
        boolean upperMatches = passwordEncoder.matches(upperCasePassword, encoded);

        // Then
        assertThat(lowerMatches).isTrue();
        assertThat(upperMatches).isFalse();
    }

    @Test
    @DisplayName("Should handle unicode characters")
    void shouldHandleUnicodeCharacters() {
        // Given
        String unicodePassword = "密码パスワード🔐";

        // When
        String encoded = passwordEncoder.encode(unicodePassword);
        boolean matches = passwordEncoder.matches(unicodePassword, encoded);

        // Then
        assertThat(encoded).isNotNull().isNotEmpty();
        assertThat(matches).isTrue();
    }
}
