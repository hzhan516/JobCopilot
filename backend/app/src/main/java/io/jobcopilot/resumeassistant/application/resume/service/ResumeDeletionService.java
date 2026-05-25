package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Handles physical and logical deletion of resume groups and versions.
 * 处理简历组和版本的物理及逻辑删除。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeDeletionService {

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;

    @Transactional(timeout = 30)
    public void deleteGroup(UUID groupId) {
        List<ResumeVersion> versions = versionRepository.findAllByGroupId(groupId);
        for (ResumeVersion v : versions) {
            if (v.getStoragePath() != null) {
                try { fileStorageService.delete(v.getStoragePath()); }
                catch (Exception e) { log.warn("Failed to delete file: {}", v.getStoragePath(), e); }
            }
        }
        versionRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(groupId);
        log.info("Resume group deleted: groupId={}", groupId);
    }

    @Transactional(timeout = 30)
    public void deleteVersion(ResumeVersion version) {
        if (version.getStoragePath() != null) {
            try { fileStorageService.delete(version.getStoragePath()); }
            catch (Exception e) { log.warn("Failed to delete file: {}", version.getStoragePath(), e); }
        }
        versionRepository.delete(version.getId());
        log.info("Resume version deleted: versionId={}", version.getId());
    }
}
