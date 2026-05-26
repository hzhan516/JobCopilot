package io.jobcopilot.resumeassistant.application.resume.service;

import io.jobcopilot.resumeassistant.application.resume.command.ResumeUploadCommand;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeGroup;
import io.jobcopilot.resumeassistant.domain.resume.entity.ResumeVersion;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import io.jobcopilot.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import io.jobcopilot.resumeassistant.domain.resume.service.ResumeConverterService;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import io.jobcopilot.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;

/**
 * Handles the initial upload flow: storage, auto-conversion, vector generation,
 * and async parse command publication.
 * 处理初始上传流程：存储、自动转换、向量生成及异步解析命令发布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadHandler {

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final ResumeConverterService resumeConverterService;
    private final VectorGenerationService vectorGenerationService;
    private final AiMessagePublisherPort aiMessagePublisherPort;

    @Transactional(timeout = 30)
    public ResumeGroup upload(ResumeUploadCommand command, UUID userId) {
        ResumeGroup group = ResumeGroup.create(userId, command.title());

        String sanitizedFileName = command.fileName() != null
                ? command.fileName().replaceAll("[:*?\"<>|\\\\/]", "_")
                : "unnamed";
        String storagePath = UUID.randomUUID().toString() + "_" + sanitizedFileName;

        fileStorageService.upload(storagePath, command.inputStream(),
                command.fileSize(), command.contentType());

        group.uploadOriginalVersion(command.fileName(), command.contentType(),
                command.fileSize(), storagePath);
        groupRepository.save(group);

        String markdown = resumeConverterService.convertToMarkdown(storagePath, command.contentType());
        ResumeVersion converted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        if (converted != null && markdown != null) {
            converted.editContent(markdown);
            versionRepository.save(converted);
            vectorGenerationService.generateForResume(converted.getId(), markdown);
            log.info("Auto-converted and vectored for groupId={}", group.getId());
        }

        ResumeVersion original = group.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Original version not found after upload"));

        original.markParsing();
        versionRepository.save(original);

        String presignedUrl = fileStorageService.generatePresignedUrl(
                original.getStoragePath(), Duration.ofHours(1));
        ResumeParseCommand parseCommand = new ResumeParseCommand(
                original.getId().toString(), presignedUrl, command.contentType());

        // Defer MQ publish until after the DB transaction commits to avoid
        // sending a message for a record that may still roll back.
        // 将 MQ 发布推迟到数据库事务提交之后，防止消息已发但记录回滚的不一致情况
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        aiMessagePublisherPort.sendResumeForParsing(parseCommand);
                        log.info("Triggered async resume parsing for versionId={}", original.getId());
                    } catch (Exception e) {
                        log.error("Failed to publish resume parsing request after commit: {}", original.getId(), e);
                    }
                }
            });
        } else {
            // Fallback when not running inside a transaction (should not happen in production)
            aiMessagePublisherPort.sendResumeForParsing(parseCommand);
            log.info("Triggered async resume parsing for versionId={}", original.getId());
        }

        log.info("Resume uploaded: groupId={}, userId={}", group.getId(), userId);
        return group;
    }
}
