package edu.asu.ser594.resumeassistant.application.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.application.resume.command.CreateVersionCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.resume.valueobject.ParseStatus;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.domain.shared.valueobject.DocumentFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates resume use cases by coordinating domain aggregates, file storage, format conversion,
 * AI-driven parsing, and vector generation. Serves as the primary application-service boundary
 * for the resume bounded context.
 * 编排简历相关用例，协调领域聚合、文件存储、格式转换、AI 解析及向量生成，作为简历限界上下文的主要应用服务边界
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeApplicationService {

    // Version chain is capped to prevent unbounded growth and excessive storage costs
    // 限制版本链长度以防止无限增长及存储成本失控
    private static final int MAX_VERSION_CHAIN_LENGTH = 50;
    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentFormatConverter documentFormatConverter;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final VectorFacade vectorFacade;

    // ==================== 命令处理 Command Handlers ====================
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeGroup handleUpload(ResumeUploadCommand command, UUID userId) {
        ResumeGroup group = ResumeGroup.create(userId, command.title());

        // Use a random UUID prefix to avoid storage key collisions across users
        // 使用随机 UUID 前缀防止不同用户间的存储键冲突
        String storagePath = UUID.randomUUID().toString() + "_" + command.fileName();

        fileStorageService.upload(storagePath, command.inputStream(),
                command.fileSize(), command.contentType());

        group.uploadOriginalVersion(command.fileName(), command.contentType(),
                command.fileSize(), storagePath);
        groupRepository.save(group);

        // Auto-convert to Markdown so users can immediately edit without waiting for AI parsing
        // 自动转换为 Markdown，使用户无需等待 AI 解析即可立即编辑
        ResumeVersion converted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        if (converted != null) {
            DocumentFormat sourceFormat = DocumentFormat.fromMimeType(command.contentType());
            try (InputStream rawStream = fileStorageService.download(storagePath).orElse(null)) {
                if (rawStream == null) {
                    log.warn("Could not download uploaded file for auto-conversion, storagePath={}", storagePath);
                } else {
                    String markdown;
                    if (!"md".equals(sourceFormat.getFormat())) {
                        try (InputStream mdStream = documentFormatConverter.convert(rawStream, sourceFormat.getFormat(), "md")) {
                            markdown = new String(mdStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } else {
                        markdown = new String(rawStream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    converted.editContent(markdown);
                    versionRepository.save(converted);
                    log.info("Auto-converted uploaded file to markdown for groupId={}", group.getId());

                    // Generate vector synchronously so semantic search works immediately after upload
                    // 同步生成向量，确保上传后立即可进行语义搜索
                    try {
                        vectorFacade.generateAndSaveVector(converted.getId().toString(), "RESUME", markdown);
                        log.info("Vector generated and saved for converted versionId={}", converted.getId());
                    } catch (Exception e) {
                        log.error("Failed to generate vector for converted versionId={}", converted.getId(), e);
                    }
                }
            } catch (IOException e) {
                converted.markParseFailed("Auto-conversion failed: " + e.getMessage());
                versionRepository.save(converted);
                log.warn("Failed to auto-convert uploaded file to markdown, leaving CONVERTED blank for groupId={}", group.getId(), e);
            }
        }

        ResumeVersion originalVersion = group.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Original version not found after upload"));

        originalVersion.markParsing();
        versionRepository.save(originalVersion);

        try {
            String presignedUrl = fileStorageService.generatePresignedUrl(
                    originalVersion.getStoragePath(), Duration.ofHours(1)
            );
            ResumeParseCommand parseCommand = new ResumeParseCommand(
                    originalVersion.getId().toString(),
                    presignedUrl,
                    command.contentType()
            );
            aiMessagePublisherPort.sendResumeForParsing(parseCommand);
            log.info("Triggered async resume parsing for versionId={}", originalVersion.getId());
        } catch (Exception e) {
            log.error("Failed to publish resume parsing request: {}", originalVersion.getId(), e);
            originalVersion.markParseFailed("Failed to publish parsing request: " + e.getMessage());
            versionRepository.save(originalVersion);
        }

        log.info("Resume uploaded: groupId={}, userId={}", group.getId(), userId);
        return group;
    }

    @Transactional
    public ResumeVersion handleEdit(ResumeEditCommand command) {
        ResumeVersion version = versionRepository.findById(command.versionId())
                .orElseThrow(() -> new StorageException("version.not.found"));

        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        if (!group.isOwnedBy(command.userId())) {
            throw new StorageException("access.denied");
        }

        version.editContent(command.content());
        versionRepository.save(version);

        log.info("Resume edited: versionId={}", version.getId());

        // Editable versions drive semantic search, so re-generate vectors on every content change
        // 可编辑版本是语义搜索的数据源，内容变更后需重新生成向量
        if (version.getVersionType() == ResumeVersion.VersionType.CONVERTED
                || version.getVersionType() == ResumeVersion.VersionType.AI_OPTIMIZED) {
            try {
                vectorFacade.generateAndSaveVector(version.getId().toString(), "RESUME", version.getContent());
                log.info("Vector generated and saved for edited resume versionId={}", version.getId());
            } catch (Exception e) {
                log.error("Failed to generate vector for versionId={}", version.getId(), e);
                // Vector generation failure is non-blocking: the edit is already persisted
                // 向量生成失败不阻塞用户：编辑内容已持久化
            }
        }

        return version;
    }

    @Transactional
    public ResumeVersion handleCreateVersion(CreateVersionCommand command) {
        ResumeGroup group = groupRepository.findByIdAndUserId(command.groupId(), command.userId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        String sourceContent = "";
        if (command.sourceVersionId() != null) {
            ResumeVersion sourceVersion = versionRepository.findById(command.sourceVersionId())
                    .orElseThrow(() -> new StorageException("version.not.found"));
            // Ensure the source version belongs to the same group to prevent cross-group data leakage
            // 确保源版本属于同一组，防止跨组数据泄露
            if (!sourceVersion.getGroupId().equals(command.groupId())) {
                throw new StorageException("version.group.mismatch");
            }
            sourceContent = sourceVersion.getContent() != null ? sourceVersion.getContent() : "";
        } else {
            ResumeVersion activeConverted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
            if (activeConverted != null) {
                sourceContent = activeConverted.getContent() != null ? activeConverted.getContent() : "";
            }
        }

        // When the version chain exceeds the limit, purge the oldest archived version to reclaim space
        // 版本链超出上限时清理最早归档的版本以回收空间
        List<ResumeVersion> convertedVersions = versionRepository.findAllByGroupIdAndType(
                command.groupId(), ResumeVersion.VersionType.CONVERTED);
        if (convertedVersions.size() >= MAX_VERSION_CHAIN_LENGTH) {
            convertedVersions.stream()
                    .filter(v -> v.getStatus() == ResumeVersion.Status.ARCHIVED)
                    .findFirst()
                    .ifPresent(oldest -> {
                        versionRepository.delete(oldest.getId());
                        log.info("Version chain limit reached. Deleted oldest archived version: versionId={}",
                                oldest.getId());
                    });
        }

        ResumeVersion newVersion = ResumeVersion.createConverted(command.groupId());
        newVersion.editContent(sourceContent);
        // If the original has already been parsed, propagate structured data to the new version immediately
        // 若原始版本已解析完成，立即将结构化数据同步到新版本
        ResumeVersion original = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);
        if (original != null && original.getParsedContent() != null && !original.getParsedContent().isEmpty()) {
            newVersion.markParseCompleted(original.getParsedContent());
        }

        if (!sourceContent.isEmpty()) {
            try {
                vectorFacade.generateAndSaveVector(newVersion.getId().toString(), "RESUME", sourceContent);
                log.info("Vector generated and saved for new converted versionId={}", newVersion.getId());
            } catch (Exception e) {
                log.error("Failed to generate vector for new converted versionId={}", newVersion.getId(), e);
            }
        }

        // Only cascade-save via groupRepository.save to avoid JPA flush ordering issues:
        // an independent save(newVersion) could cause INSERT-before-UPDATE, violating the partial unique index
        // 仅通过 groupRepository.save 级联保存，避免独立 save(newVersion) 导致 JPA flush 时 INSERT 先于旧版本 UPDATE，违反局部唯一索引
        group.addVersion(newVersion);
        groupRepository.save(group);

        log.info("Resume version created: groupId={}, newVersionId={}, sourceVersionId={}",
                command.groupId(), newVersion.getId(), command.sourceVersionId());

        return newVersion;
    }

    @Transactional
    public void handleDelete(UUID groupId, UUID userId) {
        ResumeGroup group = groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new StorageException("group.not.found"));

        List<ResumeVersion> versions = versionRepository.findAllByGroupId(groupId);

        for (ResumeVersion v : versions) {
            if (v.getStoragePath() != null) {
                try {
                    fileStorageService.delete(v.getStoragePath());
                } catch (Exception e) {
                    log.warn("Failed to delete file: {}", v.getStoragePath(), e);
                }
            }
        }

        versionRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(groupId);

        log.info("Resume group deleted: groupId={}", groupId);
    }

    @Transactional
    public void handleDeleteVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));

        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        if (!group.isOwnedBy(userId)) {
            throw new StorageException("access.denied");
        }

        // Original version is protected because it anchors the version chain and stores the raw upload
        // 原始版本受保护，因为它是版本链的锚点并保存了原始上传文件
        if (version.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            throw new StorageException("version.original.cannot.delete");
        }

        if (version.getStoragePath() != null) {
            try {
                fileStorageService.delete(version.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete file: {}", version.getStoragePath(), e);
            }
        }

        versionRepository.delete(versionId);

        log.info("Resume version deleted: versionId={}, groupId={}", versionId, group.getId());
    }

    @Transactional
    public ResumeVersion handleActivateVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));

        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        if (!group.isOwnedBy(userId)) {
            throw new StorageException("access.denied");
        }

        group.activateVersion(versionId);
        groupRepository.save(group);

        log.info("Resume version activated: versionId={}, groupId={}", versionId, group.getId());

        return versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));
    }

    @Transactional
    public void handleParseResult(AiResultEvent event) {
        ResumeVersion originalVersion = versionRepository.findById(UUID.fromString(event.referenceId()))
                .orElseThrow(() -> new StorageException("version.not.found"));

        if (!"COMPLETED".equals(event.status())) {
            originalVersion.markParseFailed(event.errorMessage() != null ? event.errorMessage() : "Unknown AI processing error");
            versionRepository.save(originalVersion);
            log.error("Resume parsing failed for versionId={}: {}", originalVersion.getId(), event.errorMessage());
            return;
        }

        try {
            // Unwrap nested wrapper object {"parsedContent": {...}, "summary": ""} to avoid storing redundant structure
            // 解包嵌套包装对象，避免存储冗余结构
            @SuppressWarnings("unchecked")
            Map<String, Object> data = event.data();
            Object parsedContentObj = data != null ? data.get("parsedContent") : null;
            String parsedJsonStr = objectMapper.writeValueAsString(parsedContentObj);
            originalVersion.markParseCompleted(parsedJsonStr);
            versionRepository.save(originalVersion);
            log.info("Resume parsing completed for versionId={}", originalVersion.getId());

            // Propagate parsed structured data to all derived versions so they share the same AI extraction
            // 将解析后的结构化数据同步到所有衍生版本，使其共享同一份 AI 提取结果
            ResumeGroup group = groupRepository.findById(originalVersion.getGroupId()).orElse(null);
            if (group != null) {
                for (ResumeVersion v : group.getVersions()) {
                    if (v.getVersionType() != ResumeVersion.VersionType.ORIGINAL
                            && v.getParseStatus() != ParseStatus.COMPLETED) {
                        v.markParseCompleted(parsedJsonStr);
                        versionRepository.save(v);
                        log.info("Copied parsed content to derived version: versionId={}", v.getId());
                    }
                }
            }

            vectorFacade.generateAndSaveVector(originalVersion.getId().toString(), "RESUME", parsedJsonStr);
            log.info("Vector generated and saved for parsed resume versionId={}", originalVersion.getId());

        } catch (Exception e) {
            log.error("Failed to process parsed data or trigger vector gen for versionId={}", originalVersion.getId(), e);
            originalVersion.markParseFailed("Failed to handle parsed result: " + e.getMessage());
            versionRepository.save(originalVersion);
        }
    }

    // ==================== 查询处理 Query Handlers ====================

    public ResumeGroup getGroup(UUID groupId, UUID userId) {
        return groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new StorageException("group.not.found"));
    }

    public List<ResumeGroup> listUserGroups(UUID userId) {
        return groupRepository.findAllByUserId(userId);
    }

    public ResumeVersion getVersion(UUID versionId, UUID userId) {
        ResumeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new StorageException("version.not.found"));

        ResumeGroup group = groupRepository.findById(version.getGroupId())
                .orElseThrow(() -> new StorageException("group.not.found"));

        if (!group.isOwnedBy(userId)) {
            throw new StorageException("access.denied");
        }

        return version;
    }

    public ResumeDownloadResult handleDownload(ResumeDownloadQuery query) {
        ResumeVersion version = getVersion(query.versionId(), query.userId());

        InputStream sourceStream;
        DocumentFormat sourceFormat;

        if (version.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            sourceStream = fileStorageService.download(version.getStoragePath())
                    .orElseThrow(() -> new StorageException("file.not.found"));
            sourceFormat = DocumentFormat.fromMimeType(version.getFileType());
        } else {
            sourceStream = new ByteArrayInputStream(
                    version.getContent().getBytes(StandardCharsets.UTF_8));
            sourceFormat = DocumentFormat.fromFormatString("md");
        }

        DocumentFormat targetFormat = DocumentFormat.fromFormatString(query.targetFormat());
        InputStream resultStream = sourceStream;
        DocumentFormat resultFormat = sourceFormat;

        if (!query.targetFormat().equalsIgnoreCase("original") && !sourceFormat.equals(targetFormat)) {
            try {
                resultStream = documentFormatConverter.convert(sourceStream, sourceFormat.getFormat(), targetFormat.getFormat());
                resultFormat = targetFormat;
            } catch (IOException e) {
                log.error("Conversion failed: {} -> {}", sourceFormat.getFormat(), targetFormat.getFormat(), e);
                throw new StorageException("conversion.failed", e);
            }
        }

        return ResumeDownloadResult.builder()
                .inputStream(resultStream)
                .fileName(resultFormat.generateOutputFileName(version.getOriginalFileName()))
                .contentType(resultFormat.getMimeType())
                .build();
    }
}
