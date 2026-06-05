package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enforces ownership rules for resume group and version access.
 * 强制执行简历组和版本访问的所有权规则。
 */
@Component
@RequiredArgsConstructor
public class ResumeAccessControl {

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;

    public ResumeGroup requireGroup(UUID groupId, UUID userId) {
        return groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new StorageException("group.not.found"));
    }

    public ResumeVersion requireVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));
        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));
        if (!group.isOwnedBy(userId)) {
            throw new StorageException("access.denied");
        }
        return version;
    }

    public ResumeGroup requireGroupForVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));
        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));
        if (!group.isOwnedBy(userId)) {
            throw new StorageException("access.denied");
        }
        return group;
    }
}
