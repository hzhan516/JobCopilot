package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.resume;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume.ResumeGroupJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.resume.ResumeGroupPersistenceMapper;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.resume.ResumeVersionPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
 * <p>
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
        when(groupMapper.toDomain(any(ResumeGroupJpaEntity.class), anyList())).thenAnswer(invocation -> {
            ResumeGroupJpaEntity e = invocation.getArgument(0);
            List<ResumeVersion> versions = invocation.getArgument(1);
            return ResumeGroup.reconstruct(e.getId(), e.getUserId(), e.getTitle(),
                    Boolean.TRUE.equals(e.getIsDefault()), e.getCreatedAt(), e.getUpdatedAt(), versions);
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

    // ==================== 重建状态保持测试 ====================
    // ==================== Reconstruction Status Tests ====================

    @Test
    @DisplayName("Should preserve mixed ACTIVE/ARCHIVED statuses on reconstruction")
    void shouldPreserveMixedActiveArchivedStatusesOnReconstruction() {
        // 给定
        // Given
        ResumeVersionJpaEntity activeJpa = ResumeVersionJpaEntity.builder()
                .id(UUID.randomUUID())
                .groupId(GROUP_ID)
                .versionType("CONVERTED")
                .status("ACTIVE")
                .fileType("text/markdown")
                .fileSize(0L)
                .parseStatus(io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResumeVersionJpaEntity archivedJpa = ResumeVersionJpaEntity.builder()
                .id(UUID.randomUUID())
                .groupId(GROUP_ID)
                .versionType("CONVERTED")
                .status("ARCHIVED")
                .fileType("text/markdown")
                .fileSize(0L)
                .parseStatus(io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ResumeVersion activeDomain = ResumeVersion.reconstruct(
                activeJpa.getId(), GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "active content", null, io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus.PENDING, null,
                ResumeVersion.Status.ACTIVE, activeJpa.getCreatedAt(), activeJpa.getUpdatedAt()
        );

        ResumeVersion archivedDomain = ResumeVersion.reconstruct(
                archivedJpa.getId(), GROUP_ID, ResumeVersion.VersionType.CONVERTED,
                null, null, "text/markdown", 0L, null, null,
                "archived content", null, io.jobcopilot.resumeassistant.domain.resume.valueobject.ParseStatus.PENDING, null,
                ResumeVersion.Status.ARCHIVED, archivedJpa.getCreatedAt(), archivedJpa.getUpdatedAt()
        );

        when(jpaRepository.findById(GROUP_ID)).thenReturn(Optional.of(testJpaEntity));
        when(jpaVersionRepository.findAllByGroupId(GROUP_ID)).thenReturn(List.of(activeJpa, archivedJpa));
        when(versionMapper.toDomain(activeJpa)).thenReturn(activeDomain);
        when(versionMapper.toDomain(archivedJpa)).thenReturn(archivedDomain);

        // 当
        // When
        Optional<ResumeGroup> result = groupRepository.findById(GROUP_ID);

        // 然后
        // Then
        assertThat(result).isPresent();
        ResumeGroup group = result.get();
        List<ResumeVersion> versions = group.getVersions();
        assertThat(versions).hasSize(2);
        assertThat(versions).extracting(ResumeVersion::getStatus)
                .containsExactlyInAnyOrder(ResumeVersion.Status.ACTIVE, ResumeVersion.Status.ARCHIVED);
    }
}
