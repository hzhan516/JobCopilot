package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserCredential Entity Unit Tests
 * 
 * Tests the UserCredential entity following DDD principles:
 * - Immutable credential value after creation
 * - Factory method for password credentials
 * - Builder pattern
 */
@DisplayName("UserCredential Entity Tests")
class UserCredentialTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String HASHED_PASSWORD = "$2a$10$hashedpassword123...";

    @Test
    @DisplayName("Should create password credential with factory method")
    void shouldCreatePasswordCredentialWithFactoryMethod() {
        // When
        UserCredential credential = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD);

        // Then
        assertThat(credential).isNotNull();
        assertThat(credential.getId()).isNotNull();
        assertThat(credential.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(credential.getCredentialType()).isEqualTo(CredentialType.PASSWORD);
        assertThat(credential.getCredentialValue()).isEqualTo(HASHED_PASSWORD);
        assertThat(credential.getLastChangedAt()).isNotNull();
        assertThat(credential.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should generate unique ID for each credential")
    void shouldGenerateUniqueIdForEachCredential() {
        // When
        UserCredential credential1 = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD);
        UserCredential credential2 = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD + "2");

        // Then
        assertThat(credential1.getId()).isNotEqualTo(credential2.getId());
    }

    @Test
    @DisplayName("Should build credential with builder pattern")
    void shouldBuildCredentialWithBuilderPattern() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // When
        UserCredential credential = UserCredential.builder()
                .id(id)
                .userId(TEST_USER_ID)
                .credentialType(CredentialType.PASSWORD)
                .credentialValue(HASHED_PASSWORD)
                .lastChangedAt(now)
                .createdAt(now)
                .build();

        // Then
        assertThat(credential.getId()).isEqualTo(id);
        assertThat(credential.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(credential.getCredentialType()).isEqualTo(CredentialType.PASSWORD);
        assertThat(credential.getCredentialValue()).isEqualTo(HASHED_PASSWORD);
    }

    @Test
    @DisplayName("Should maintain immutability of all fields")
    void shouldMaintainImmutabilityOfAllFields() {
        // Given
        UserCredential credential = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD);
        UUID originalId = credential.getId();
        UUID originalUserId = credential.getUserId();
        CredentialType originalType = credential.getCredentialType();
        String originalValue = credential.getCredentialValue();
        LocalDateTime originalLastChanged = credential.getLastChangedAt();
        LocalDateTime originalCreated = credential.getCreatedAt();

        // Then - all getters return the same values
        assertThat(credential.getId()).isEqualTo(originalId);
        assertThat(credential.getUserId()).isEqualTo(originalUserId);
        assertThat(credential.getCredentialType()).isEqualTo(originalType);
        assertThat(credential.getCredentialValue()).isEqualTo(originalValue);
        assertThat(credential.getLastChangedAt()).isEqualTo(originalLastChanged);
        assertThat(credential.getCreatedAt()).isEqualTo(originalCreated);
    }

    @Test
    @DisplayName("Should implement Entity interface correctly")
    void shouldImplementEntityInterfaceCorrectly() {
        // Given
        UserCredential credential = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD);

        // Then
        assertThat(credential.getId()).isNotNull();
        assertThat(credential).isInstanceOf(edu.asu.ser594.resumeassistant.domain.shared.entity.Entity.class);
    }

    @Test
    @DisplayName("Should set timestamps on creation")
    void shouldSetTimestampsOnCreation() {
        // When
        UserCredential credential = UserCredential.createPassword(TEST_USER_ID, HASHED_PASSWORD);

        // Then
        assertThat(credential.getCreatedAt()).isNotNull();
        assertThat(credential.getLastChangedAt()).isNotNull();
        assertThat(credential.getCreatedAt()).isEqualTo(credential.getLastChangedAt());
    }

    @Test
    @DisplayName("Should store different credential values")
    void shouldStoreDifferentCredentialValues() {
        // Given
        String password1 = "hashed_password_1";
        String password2 = "hashed_password_2";

        // When
        UserCredential credential1 = UserCredential.createPassword(TEST_USER_ID, password1);
        UserCredential credential2 = UserCredential.createPassword(TEST_USER_ID, password2);

        // Then
        assertThat(credential1.getCredentialValue()).isEqualTo(password1);
        assertThat(credential2.getCredentialValue()).isEqualTo(password2);
        assertThat(credential1.getCredentialValue()).isNotEqualTo(credential2.getCredentialValue());
    }
}
