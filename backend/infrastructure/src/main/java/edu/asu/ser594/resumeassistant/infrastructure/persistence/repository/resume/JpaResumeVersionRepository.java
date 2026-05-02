package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.resume;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeVersionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历版本Spring Data JPA仓库
 */
@Repository
public interface JpaResumeVersionRepository extends JpaRepository<ResumeVersionJpaEntity, UUID> {

    List<ResumeVersionJpaEntity> findAllByGroupId(UUID groupId);

    Optional<ResumeVersionJpaEntity> findByIdAndGroupId(UUID id, UUID groupId);

    @Query("SELECT v FROM ResumeVersionJpaEntity v WHERE v.groupId = :groupId AND v.versionType = :type AND v.status = 'ACTIVE'")
    Optional<ResumeVersionJpaEntity> findActiveByGroupIdAndType(@Param("groupId") UUID groupId, @Param("type") String type);

    @Modifying
    @Query("UPDATE ResumeVersionJpaEntity v SET v.status = 'ARCHIVED' WHERE v.groupId = :groupId AND v.versionType = :type")
    void archiveByGroupIdAndType(@Param("groupId") UUID groupId, @Param("type") String type);

    @Modifying
    @Query("DELETE FROM ResumeVersionJpaEntity v WHERE v.groupId = :groupId")
    void deleteAllByGroupId(@Param("groupId") UUID groupId);

    List<ResumeVersionJpaEntity> findAllByGroupIdAndVersionTypeOrderByCreatedAtAsc(UUID groupId, String versionType);
}
