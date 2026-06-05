package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.command.CreateVersionCommand;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the version chain for resume groups, enforcing the max chain length
 * and sourcing content from the active converted version or a specific source.
 * 管理简历组的版本链，强制执行最大链长度，并从活跃转换版本或指定源获取内容。
 */
@Component
@RequiredArgsConstructor
public class ResumeVersionChainManager {

    private static final int MAX_VERSION_CHAIN_LENGTH = 50;

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final VectorGenerationService vectorGenerationService;

    @Transactional(timeout = 30)
    public ResumeVersion createVersion(CreateVersionCommand command) {
        ResumeGroup group = groupRepository.findByIdAndUserId(command.groupId(), command.userId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        String sourceContent = resolveSourceContent(command, group);
        enforceChainLimit(command.groupId());

        ResumeVersion newVersion = ResumeVersion.createConverted(command.groupId());
        newVersion.editContent(sourceContent);

        ResumeVersion original = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);
        if (original != null && original.getParsedContent() != null && !original.getParsedContent().isEmpty()) {
            newVersion.markParseCompleted(original.getParsedContent());
        }

        if (!sourceContent.isEmpty()) {
            vectorGenerationService.generateForResume(newVersion.getId(), sourceContent);
        }

        group.addVersion(newVersion);
        groupRepository.save(group);

        return newVersion;
    }

    private String resolveSourceContent(CreateVersionCommand command, ResumeGroup group) {
        if (command.sourceVersionId() != null) {
            ResumeVersion sourceVersion = versionRepository.findById(command.sourceVersionId())
                    .orElseThrow(() -> new StorageException("version.not.found"));
            if (!sourceVersion.getGroupId().equals(command.groupId())) {
                throw new StorageException("version.group.mismatch");
            }
            return sourceVersion.getContent() != null ? sourceVersion.getContent() : "";
        }

        ResumeVersion activeConverted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        return activeConverted != null && activeConverted.getContent() != null
                ? activeConverted.getContent() : "";
    }

    private void enforceChainLimit(UUID groupId) {
        List<ResumeVersion> convertedVersions = versionRepository.findAllByGroupIdAndType(
                groupId, ResumeVersion.VersionType.CONVERTED);
        if (convertedVersions.size() >= MAX_VERSION_CHAIN_LENGTH) {
            convertedVersions.stream()
                    .filter(v -> v.getStatus() == ResumeVersion.Status.ARCHIVED)
                    .findFirst()
                    .ifPresent(oldest -> {
                        versionRepository.delete(oldest.getId());
                    });
        }
    }
}
