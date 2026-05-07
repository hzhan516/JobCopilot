package edu.asu.ser594.resumeassistant.domain.resume.repository;

import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历版本仓储接口
 * Resume Version Repository Interface
 */
public interface ResumeVersionRepository {

    /**
     * 保存版本
     * Save version
     */
    void save(ResumeVersion version);

    /**
     * 批量保存
     * Batch save
     */
    void saveAll(List<ResumeVersion> versions);

    /**
     * 根据ID查找
     * Find by ID
     */
    Optional<ResumeVersion> findById(UUID versionId);

    /**
     * 查找组的所有版本
     * Find all versions in group
     */
    List<ResumeVersion> findAllByGroupId(UUID groupId);

    /**
     * 查找指定类型的ACTIVE版本
     */
    Optional<ResumeVersion> findActiveByGroupIdAndType(UUID groupId,
                                                       ResumeVersion.VersionType type);

    /**
     * 删除版本
     * Delete version
     */
    void delete(UUID versionId);

    /**
     * 删除组的所有版本
     * Delete all versions in group
     */
    void deleteAllByGroupId(UUID groupId);

    /**
     * 查找组内指定类型的所有版本（含 ARCHIVED）
     * Find all versions of specified type in group (including ARCHIVED)
     *
     * @param groupId 组ID / Group ID
     * @param type    版本类型 / Version type
     * @return 版本列表，按 createdAt 升序 / Version list sorted by createdAt ascending
     */
    List<ResumeVersion> findAllByGroupIdAndType(UUID groupId, ResumeVersion.VersionType type);
}
