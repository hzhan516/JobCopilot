package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeGroupPersistenceMapper;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume.JpaResumeGroupRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume.JpaResumeVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ResumeGroupRepositoryImpl 单元测试
 * ResumeGroupRepositoryImpl Unit Tests
 * 
 * 测试简历组仓储实现：
 * Tests the resume group repository implementation:
 * - CRUD 操作
 * - CRUD operations
 * - 基于用户的查询
 * - User-based queries
 * - 版本关系处理
 * - Version relationship handling
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Resume Group Repository Implementation Tests")
class ResumeGroupRepositoryImplTest {

    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private JpaResumeGroupRepository jpaRepository;

    @Mock
    private JpaResumeVersionRepository jpaVersionRepository;

    @Mock
    private ResumeGroupPersistenceMapper groupMapper;

    @Mock
    private ResumeVersionPersistenceMapper versionMapper;

    @InjectMocks
    private ResumeGroupRepositoryImpl groupRepository;

    private ResumeGroup testGroup;
    private ResumeGroupJpaEntity testJpaEntity;

    @BeforeEach
    void setUp() {
        testGroup = ResumeGroup.create(USER_ID, "Test Resume");

        testJpaEntity = new ResumeGroupJpaEntity();
        testJpaEntity.setId(GROUP_ID);
        testJpaEntity.setUserId(USER_ID);
        testJpaEntity.setTitle("Test Resume");
        testJpaEntity.setIsDefault(false);
        testJpaEntity.setCreatedAt(LocalDateTime.now());
        testJpaEntity.setUpdatedAt(LocalDateTime.now());

        when(groupMapper.toJpaEntity(any(ResumeGroup.class))).thenReturn(testJpaEntity);
        when(groupMapper.toDomain(any(ResumeGroupJpaEntity.class))).thenAnswer(invocation -> {
            ResumeGroupJpaEntity e = invocation.getArgument(0);
            return ResumeGroup.reconstruct(e.getId(), e.getUserId(), e.getTitle(),
                    Boolean.TRUE.equals(e.getIsDefault()), e.getCreatedAt(), e.getUpdatedAt(), java.util.Collections.emptyList());
        });
        when(versionMapper.toJpaEntity(any(ResumeVersion.class))).thenReturn(new ResumeVersionJpaEntity());
        when(versionMapper.toDomain(any(ResumeVersionJpaEntity.class))).thenReturn(
                ResumeVersion.createConverted(GROUP_ID));
    }

    // ==================== 保存测试 ====================
    // ==================== Save Tests ====================

    @Test
    @DisplayName("Should save resume group")
    void shouldSaveResumeGroup() {
        // 给定
        // Given
        when(jpaRepository.save(any(ResumeGroupJpaEntity.class))).thenReturn(testJpaEntity);

        // 当
        // When
        groupRepository.save(testGroup);

        // 然后
        // Then
        verify(jpaRepository).save(any(ResumeGroupJpaEntity.class));
    }

    // ==================== 按 ID 查询测试 ====================
    // ==================== Find By ID Tests ====================

    @Test
    @DisplayName("Should find group by ID")
    void shouldFindGroupById() {
        // 给定
        // Given
        when(jpaRepository.findById(GROUP_ID)).thenReturn(Optional.of(testJpaEntity));

        // 当
        // When
        Optional<ResumeGroup> result = groupRepository.findById(GROUP_ID);

        // 然后
        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should return empty when group not found by ID")
    void shouldReturnEmptyWhenGroupNotFoundById() {
        // 给定
        // Given
        when(jpaRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        // 当
        // When
        Optional<ResumeGroup> result = groupRepository.findById(GROUP_ID);

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 按 ID 和用户 ID 查询测试 ====================
    // ==================== Find By ID and User ID Tests ====================

    @Test
    @DisplayName("Should find group by ID and user ID")
    void shouldFindGroupByIdAndUserId() {
        // 给定
        // Given
        when(jpaRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testJpaEntity));

        // 当
        // When
        Optional<ResumeGroup> result = groupRepository.findByIdAndUserId(GROUP_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should return empty when group not found for user")
    void shouldReturnEmptyWhenGroupNotFoundForUser() {
        // 给定
        // Given
        when(jpaRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.empty());

        // 当
        // When
        Optional<ResumeGroup> result = groupRepository.findByIdAndUserId(GROUP_ID, USER_ID);

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 按用户 ID 查询全部测试 ====================
    // ==================== Find All By User ID Tests ====================

    @Test
    @DisplayName("Should find all groups by user ID")
    void shouldFindAllGroupsByUserId() {
        // 给定
        // Given
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of(testJpaEntity));

        // 当
        // When
        List<ResumeGroup> result = groupRepository.findAllByUserId(USER_ID);

        // 然后
        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when no groups found for user")
    void shouldReturnEmptyListWhenNoGroupsFoundForUser() {
        // 给定
        // Given
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

        // 当
        // When
        List<ResumeGroup> result = groupRepository.findAllByUserId(USER_ID);

        // 然后
        // Then
        assertThat(result).isEmpty();
    }

    // ==================== 删除测试 ====================
    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should delete group by ID")
    void shouldDeleteGroupById() {
        // 当
        // When
        groupRepository.delete(GROUP_ID);

        // 然后
        // Then
        verify(jpaRepository).deleteById(GROUP_ID);
    }

    @Test
    @DisplayName("Should handle delete of non-existent group")
    void shouldHandleDeleteOfNonExistentGroup() {
        // 给定
        // Given
        doNothing().when(jpaRepository).deleteById(GROUP_ID);

        // 当
        // When
        groupRepository.delete(GROUP_ID);

        // 然后
        // Then
        verify(jpaRepository).deleteById(GROUP_ID);
    }
}
