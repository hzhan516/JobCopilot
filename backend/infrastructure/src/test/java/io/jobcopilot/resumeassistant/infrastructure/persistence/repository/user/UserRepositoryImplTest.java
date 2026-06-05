package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.user;

import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.UserPersistenceMapper;
import io.jobcopilot.resumeassistant.types.enums.UserRole;
import io.jobcopilot.resumeassistant.types.enums.UserStatus;
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
 * UserRepositoryImpl 单元测试
 * UserRepositoryImpl Unit Tests
 * <p>
 * 测试用户仓储实现：
 * Tests the user repository implementation:
 * - CRUD 操作
 * - CRUD operations
 * - 领域到 JPA 实体映射
 * - Domain to JPA entity mapping
 * - 仓储委托
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
        testJpaEntity.setVersion(0L);
    }

    // ==================== 保存测试 ====================
    // ==================== Save Tests ====================

    @Test
    @DisplayName("Should save user and return domain object")
    void shouldSaveUserAndReturnDomainObject() {
        // 给定
        // Given
        when(mapper.toJpaEntity(testUser)).thenReturn(testJpaEntity);
        when(jpaRepository.save(testJpaEntity)).thenReturn(testJpaEntity);
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        User result = userRepository.save(testUser);

        // 然后
        // Then
        assertThat(result).isEqualTo(testUser);
        verify(mapper).toJpaEntity(testUser);
        verify(jpaRepository).save(testJpaEntity);
        verify(mapper).toDomain(testJpaEntity);
    }

    @Test
    @DisplayName("Should clear version when saving new assigned-ID user")
    void shouldClearVersionWhenSavingNewAssignedIdUser() {
        // 给定
        // Given
        when(mapper.toJpaEntity(testUser)).thenReturn(testJpaEntity);
        when(jpaRepository.existsById(USER_ID)).thenReturn(false);
        when(jpaRepository.save(testJpaEntity)).thenReturn(testJpaEntity);
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        userRepository.save(testUser);

        // 然后
        // Then
        assertThat(testJpaEntity.getVersion()).isNull();
        verify(jpaRepository).existsById(USER_ID);
    }

    @Test
    @DisplayName("Should keep version when saving existing user")
    void shouldKeepVersionWhenSavingExistingUser() {
        // 给定
        // Given
        testJpaEntity.setVersion(7L);
        when(mapper.toJpaEntity(testUser)).thenReturn(testJpaEntity);
        when(jpaRepository.existsById(USER_ID)).thenReturn(true);
        when(jpaRepository.save(testJpaEntity)).thenReturn(testJpaEntity);
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        userRepository.save(testUser);

        // 然后
        // Then
        assertThat(testJpaEntity.getVersion()).isEqualTo(7L);
        verify(jpaRepository).existsById(USER_ID);
    }

    @Test
    @DisplayName("Should map domain to entity before saving")
    void shouldMapDomainToEntityBeforeSaving() {
        // 给定
        // Given
        when(mapper.toJpaEntity(any(User.class))).thenReturn(testJpaEntity);
        when(jpaRepository.save(any())).thenReturn(testJpaEntity);
        when(mapper.toDomain(any())).thenReturn(testUser);

        // 当
        // When
        userRepository.save(testUser);

        // 然后
        // Then
        verify(mapper).toJpaEntity(testUser);
    }

    // ==================== 按 ID 查询测试 ====================
    // ==================== Find By ID Tests ====================

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        // 给定
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        Optional<User> result = userRepository.findById(USER_ID);

        // 然后
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void shouldReturnEmptyWhenUserNotFoundById() {
        // 给定
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // 当
        // When
        Optional<User> result = userRepository.findById(USER_ID);

        // 然后
        // Then
        assertThat(result).isEmpty();
        verify(mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("Should map found entity to domain")
    void shouldMapFoundEntityToDomain() {
        // 给定
        // Given
        when(jpaRepository.findById(USER_ID)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        userRepository.findById(USER_ID);

        // 然后
        // Then
        verify(mapper).toDomain(testJpaEntity);
    }

    // ==================== 按邮箱查询测试 ====================
    // ==================== Find By Email Tests ====================

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // 给定
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testJpaEntity));
        when(mapper.toDomain(testJpaEntity)).thenReturn(testUser);

        // 当
        // When
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // 然后
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // 给定
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // 当
        // When
        Optional<User> result = userRepository.findByEmail(TEST_EMAIL);

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delegate email lookup to JPA repository")
    void shouldDelegateEmailLookupToJpaRepository() {
        // 给定
        // Given
        when(jpaRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // 当
        // When
        userRepository.findByEmail(TEST_EMAIL);

        // 然后
        // Then
        verify(jpaRepository).findByEmail(TEST_EMAIL);
    }

    // ==================== 按邮箱存在性测试 ====================
    // ==================== Exists By Email Tests ====================

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        // 给定
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // 当
        // When
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);

        // 然后
        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // 给定
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // 当
        // When
        boolean exists = userRepository.existsByEmail(TEST_EMAIL);

        // 然后
        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should delegate exists check to JPA repository")
    void shouldDelegateExistsCheckToJpaRepository() {
        // 给定
        // Given
        when(jpaRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // 当
        // When
        userRepository.existsByEmail(TEST_EMAIL);

        // 然后
        // Then
        verify(jpaRepository).existsByEmail(TEST_EMAIL);
    }
}
