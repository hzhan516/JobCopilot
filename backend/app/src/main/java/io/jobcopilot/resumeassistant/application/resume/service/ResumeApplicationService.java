package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.command.CreateVersionCommand;
import io.jobcopilot.resumeassistant.application.resume.command.ResumeEditCommand;
import io.jobcopilot.resumeassistant.application.resume.command.ResumeUploadCommand;
import io.jobcopilot.resumeassistant.application.resume.dto.ResumeDownloadResult;
import io.jobcopilot.resumeassistant.application.resume.query.ResumeDownloadQuery;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import io.jobcopilot.resumeassistant.domain.shared.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Thin orchestrator for the resume bounded context. Delegates to domain services
 * and extracted handlers for complex workflows.
 * 简历限界上下文的薄层编排器。将复杂工作流委托给领域服务和提取的处理器。
 */
@Slf4j
@Service
public class ResumeApplicationService {

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final ResumeUploadHandler uploadHandler;
    private final VectorGenerationService vectorGenerationService;
    private final ResumeParseResultHandler parseResultHandler;
    private final ResumeVersionChainManager versionChainManager;
    private final ResumeDownloadService downloadService;
    private final ResumeDeletionService deletionService;
    private final ResumeAccessControl accessControl;

    public ResumeApplicationService(ResumeGroupRepository groupRepository,
                                    ResumeVersionRepository versionRepository,
                                    ResumeUploadHandler uploadHandler,
                                    VectorGenerationService vectorGenerationService,
                                    ResumeParseResultHandler parseResultHandler,
                                    ResumeVersionChainManager versionChainManager,
                                    ResumeDownloadService downloadService,
                                    ResumeDeletionService deletionService,
                                    ResumeAccessControl accessControl) {
        this.groupRepository = groupRepository;
        this.versionRepository = versionRepository;
        this.uploadHandler = uploadHandler;
        this.vectorGenerationService = vectorGenerationService;
        this.parseResultHandler = parseResultHandler;
        this.versionChainManager = versionChainManager;
        this.downloadService = downloadService;
        this.deletionService = deletionService;
        this.accessControl = accessControl;
    }

    @Transactional(timeout = 30)
    public ResumeGroup handleUpload(ResumeUploadCommand command, UUID userId) {
        return uploadHandler.upload(command, userId);
    }

    @Transactional(timeout = 30)
    public ResumeVersion handleEdit(ResumeEditCommand command) {
        ResumeVersion version = accessControl.requireVersion(command.versionId(), command.userId());
        version.editContent(command.content());
        versionRepository.save(version);

        if (version.getVersionType() == ResumeVersion.VersionType.CONVERTED
                || version.getVersionType() == ResumeVersion.VersionType.AI_OPTIMIZED) {
            // Defer vector generation until after DB commit to avoid holding a long transaction
            // 将向量生成推迟到数据库事务提交之后，避免持有长事务
            deferVectorGeneration(version.getId(), version.getContent());
        }

        log.info("Resume edited: versionId={}", version.getId());
        return version;
    }

    /**
     * Defer vector generation until the current DB transaction commits.
     * 将向量生成推迟到当前数据库事务提交之后执行。
     */
    private void deferVectorGeneration(UUID versionId, String content) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        vectorGenerationService.generateForResume(versionId, content);
                        log.info("Vector generation triggered after commit for version: {}", versionId);
                    } catch (Exception e) {
                        log.error("Vector generation failed after commit for version: {}", versionId, e);
                    }
                }
            });
        } else {
            vectorGenerationService.generateForResume(versionId, content);
        }
    }

    @Transactional(timeout = 30)
    public ResumeVersion handleCreateVersion(CreateVersionCommand command) {
        return versionChainManager.createVersion(command);
    }

    @Transactional(timeout = 30)
    public void handleDelete(UUID groupId, UUID userId) {
        accessControl.requireGroup(groupId, userId);
        deletionService.deleteGroup(groupId);
    }

    @Transactional(timeout = 30)
    public void handleDeleteVersion(UUID versionId, UUID userId) {
        ResumeVersion version = accessControl.requireVersion(versionId, userId);

        if (version.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            throw new StorageException("version.original.cannot.delete");
        }

        deletionService.deleteVersion(version);
    }

    @Transactional(timeout = 30)
    public ResumeVersion handleActivateVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));
        ResumeGroup group = accessControl.requireGroup(version.getGroupId(), userId);

        group.activateVersion(versionId);
        groupRepository.save(group);
        log.info("Resume version activated: versionId={}", versionId);

        return versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));
    }

    @Transactional(timeout = 30)
    public void handleParseResult(AiResultEvent event) {
        log.info("Handling parse result for ResumeVersion: {}", event.referenceId());
        parseResultHandler.handle(event);
    }

    @Transactional(readOnly = true)
    public ResumeGroup getGroup(UUID groupId, UUID userId) {
        return accessControl.requireGroup(groupId, userId);
    }

    @Transactional(readOnly = true)
    public List<ResumeGroup> listUserGroups(UUID userId) {
        return groupRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ResumeVersion getVersion(UUID versionId, UUID userId) {
        return accessControl.requireVersion(versionId, userId);
    }

    public ResumeDownloadResult handleDownload(ResumeDownloadQuery query) {
        ResumeVersion version = accessControl.requireVersion(query.versionId(), query.userId());
        return downloadService.download(version, query.targetFormat());
    }
}
