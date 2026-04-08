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
     */
    void save(ResumeVersion version);

    /**
     * 批量保存
     */
    void saveAll(List<ResumeVersion> versions);

    /**
     * 根据ID查找
     */
    Optional<ResumeVersion> findById(UUID versionId);

    /**
     * 查找组的所有版本
     */
    List<ResumeVersion> findAllByGroupId(UUID groupId);

    /**
     * 查找指定类型的ACTIVE版本
     */
    Optional<ResumeVersion> findActiveByGroupIdAndType(UUID groupId,
                                                       ResumeVersion.VersionType type);

    /**
     * 删除版本
     */
    void delete(UUID versionId);

    /**
     * 删除组的所有版本
     */
    void deleteAllByGroupId(UUID groupId);
}
