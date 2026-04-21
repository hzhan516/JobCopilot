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
 * ResumeGroupRepositoryImpl Unit Tests
 * 
 * Tests the resume group repository implementation:
 * - CRUD operations
 * - User-based queries
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

    // ==================== Save Tests ====================

    @Test
    @DisplayName("Should save resume group")
    void shouldSaveResumeGroup() {
        // Given
        when(jpaRepository.save(any(ResumeGroupJpaEntity.class))).thenReturn(testJpaEntity);

        // When
        groupRepository.save(testGroup);

        // Then
        verify(jpaRepository).save(any(ResumeGroupJpaEntity.class));
    }

    // ==================== Find By ID Tests ====================

    @Test
    @DisplayName("Should find group by ID")
    void shouldFindGroupById() {
        // Given
        when(jpaRepository.findById(GROUP_ID)).thenReturn(Optional.of(testJpaEntity));

        // When
        Optional<ResumeGroup> result = groupRepository.findById(GROUP_ID);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should return empty when group not found by ID")
    void shouldReturnEmptyWhenGroupNotFoundById() {
        // Given
        when(jpaRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        // When
        Optional<ResumeGroup> result = groupRepository.findById(GROUP_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Find By ID and User ID Tests ====================

    @Test
    @DisplayName("Should find group by ID and user ID")
    void shouldFindGroupByIdAndUserId() {
        // Given
        when(jpaRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.of(testJpaEntity));

        // When
        Optional<ResumeGroup> result = groupRepository.findByIdAndUserId(GROUP_ID, USER_ID);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should return empty when group not found for user")
    void shouldReturnEmptyWhenGroupNotFoundForUser() {
        // Given
        when(jpaRepository.findByIdAndUserId(GROUP_ID, USER_ID)).thenReturn(Optional.empty());

        // When
        Optional<ResumeGroup> result = groupRepository.findByIdAndUserId(GROUP_ID, USER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Find All By User ID Tests ====================

    @Test
    @DisplayName("Should find all groups by user ID")
    void shouldFindAllGroupsByUserId() {
        // Given
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(List.of(testJpaEntity));

        // When
        List<ResumeGroup> result = groupRepository.findAllByUserId(USER_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when no groups found for user")
    void shouldReturnEmptyListWhenNoGroupsFoundForUser() {
        // Given
        when(jpaRepository.findAllByUserId(USER_ID)).thenReturn(Collections.emptyList());

        // When
        List<ResumeGroup> result = groupRepository.findAllByUserId(USER_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Delete Tests ====================

    @Test
    @DisplayName("Should delete group by ID")
    void shouldDeleteGroupById() {
        // When
        groupRepository.delete(GROUP_ID);

        // Then
        verify(jpaRepository).deleteById(GROUP_ID);
    }

    @Test
    @DisplayName("Should handle delete of non-existent group")
    void shouldHandleDeleteOfNonExistentGroup() {
        // Given
        doNothing().when(jpaRepository).deleteById(GROUP_ID);

        // When
        groupRepository.delete(GROUP_ID);

        // Then
        verify(jpaRepository).deleteById(GROUP_ID);
    }
}
