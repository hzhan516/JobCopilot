package edu.asu.ser594.resumeassistant.application.resume.service;

import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeGroupRepository;
import edu.asu.ser594.resumeassistant.domain.resume.repository.ResumeVersionRepository;
import edu.asu.ser594.resumeassistant.domain.shared.port.AiMessagePublisherPort;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.ResumeParseCommand;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.VectorGenCommand;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import edu.asu.ser594.resumeassistant.domain.shared.service.DocumentFormatConverter;
import edu.asu.ser594.resumeassistant.domain.shared.service.FileStorageService;
import edu.asu.ser594.resumeassistant.domain.shared.valueobject.DocumentFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Resume Application Service
 *
 * 职责：编排用例，协调领域对象
 * Responsibility: Orchestrate use cases, coordinate domain objects
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeApplicationService {

    private final ResumeGroupRepository groupRepository;
    private final ResumeVersionRepository versionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentFormatConverter documentFormatConverter;
    private final AiMessagePublisherPort aiMessagePublisherPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 命令处理 Command Handlers ====================

    @Transactional
    public ResumeGroup handleUpload(ResumeUploadCommand command, UUID userId) {
        // 1. 创建简历组
        ResumeGroup group = ResumeGroup.create(userId, command.title());

        // 2. 存储文件，获取路径（这让底层基础设施去决定真实存的位置，但如果接口设计需要传入Path，则生成随机键）
        String storagePath = UUID.randomUUID().toString() + "_" + command.fileName();
        
        fileStorageService.upload(storagePath, command.inputStream(),
                command.fileSize(), command.contentType());

        // 3. 将原版添加进简历组（包含创建衍生版本等生命周期都在聚合内）
        group.uploadOriginalVersion(command.fileName(), command.contentType(),
                command.fileSize(), storagePath);

        // 4. 保存聚合
        groupRepository.save(group);

        // 5. 触发 AI 异步解析
        ResumeVersion originalVersion = group.getVersions().stream()
                .filter(v -> v.getVersionType() == ResumeVersion.VersionType.ORIGINAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Original version not found after upload"));
        
        originalVersion.markParsing();
        versionRepository.save(originalVersion);

        try {
            ResumeParseCommand parseCommand = new ResumeParseCommand(
                    originalVersion.getId().toString(),
                    originalVersion.getStoragePath(),
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

        // 调用领域方法
        version.editContent(command.content());
        versionRepository.save(version);

        log.info("Resume edited: versionId={}", version.getId());
        return version;
    }

    @Transactional
    public void handleDelete(UUID groupId, UUID userId) {
        ResumeGroup group = groupRepository.findByIdAndUserId(groupId, userId)
                .orElseThrow(() -> new StorageException("group.not.found"));

        List<ResumeVersion> versions = versionRepository.findAllByGroupId(groupId);

        // 删除文件
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

        // 如果是 ORIGINAL 版本，不允许单独删除（必须通过删除组来删除）
        if (version.getVersionType() == ResumeVersion.VersionType.ORIGINAL) {
            throw new StorageException("version.original.cannot.delete");
        }

        // 删除文件（如果有存储路径）
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
            String parsedJsonStr = objectMapper.writeValueAsString(event.data());
            originalVersion.markParseCompleted(parsedJsonStr);
            versionRepository.save(originalVersion);
            log.info("Resume parsing completed for versionId={}", originalVersion.getId());

            // 触发向量生成
            VectorGenCommand vectorCmd = new VectorGenCommand(
                    originalVersion.getId().toString(),
                    "RESUME",
                    parsedJsonStr
            );
            aiMessagePublisherPort.sendTextForVectorGeneration(vectorCmd);
            log.info("Triggered async vector generation for resume versionId={}", originalVersion.getId());

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

        // 格式转换
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