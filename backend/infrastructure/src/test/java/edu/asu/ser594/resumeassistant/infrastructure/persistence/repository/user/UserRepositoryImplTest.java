package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.UserPersistenceMapper;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserRepositoryImpl Unit Tests
 * 
 * Tests the user repository implementation:
 * - CRUD operations
 * - Domain to JPA entity mapping
 * - Repository delegation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("User Repository Implementation Tests")
class UserRepositoryImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@example.com";

    @Mock
    private JpaUserRepository jpaRepository;

    @Mock
    private UserPersistenceMapper mapper;

    @InjectMocks
    private UserRepositoryImpl userRepository;

    private User testUser;
    private UserJpaEntity testJpaEntity;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email(TEST_EMAIL)
                .emailVerified(false)
                .role(UserRole.JOB_SEEKER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testJpaEntity = new UserJpaEntity();
        testJpaEntity.setId(USER_ID);
        testJpaEntity.setEmail(TEST_EMAIL);
        testJpaEntity.setEmailVerified(false);
        testJpaEntity.setRole(UserRole.JOB_SEEKER);
        testJpaEntity.setStatus(UserStatus.ACTIVE);
    }

    // ==================== Save Tests ====================

    @Test
    @DisplayName("Should save user and return domain object")
    void shouldSaveUserAndReturnDomainObject() {
        // Given
        when(mapper.toJpaEntity(testUser)).thenReturn(testJpaEntity);
        when(jpaRepository.save(testJpaEntity)).thenReturn(testJpaEntity);
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // When
        User result = userRepository.save(testUser);

        // Then
        assertThat(result).isEqualTo(testUser);
        verify(mapper).toJpaEntity(testUser);
        verify(jpaRepository).save(testJpaEntity);
        verify(mapper).toDomain(testJpaEntity);
    }

    @Test
    @DisplayName("Should map domain to entity before saving")
    void shouldMapDomainToEntityBeforeSaving() {
        // Given
        when(mapper.toJpaEntity(any(User.class))).thenReturn(testJpaEntity);
        when(jpaRepository.save(any())).thenReturn(testJpaEntity);
        when(mapper.toDomain(any())).thenReturn(testUser);

        // When
        userRepository.save(testUser);

        // Then
        verify(mapper).toJpaEntity(testUser);
    }

    // ==================== Find By ID Tests ====================

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // When
        Optional<User> result = userRepository.findById(USER_ID);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void shouldReturnEmptyWhenUserNotFoundById() {
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userRepository.findById(USER_ID);

        // Then
        assertThat(result).isEmpty();
        verify(mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("Should map found entity to domain")
    void shouldMapFoundEntityToDomain() {
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // When
        userRepository.findById(USER_ID);

        // Then
        verify(mapper).toDomain(testJpaEntity);
    }

    // ==================== Find By Email Tests ====================

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // When
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delegate email lookup to JPA repository")
    void shouldDelegateEmailLookupToJpaRepository() {
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When
        userRepository.findByEmail(TEST_EMAIL);

        // Then
        verify(jpaRepository).findByEmail(TEST_EMAIL);
    }

    // ==================== Exists By Email Tests ====================

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // When
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should delegate exists check to JPA repository")
    void shouldDelegateExistsCheckToJpaRepository() {
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When
        userRepository.existsByEmail(TEST_EMAIL);

        // Then
        verify(jpaRepository).existsByEmail(TEST_EMAIL);
    }
}
