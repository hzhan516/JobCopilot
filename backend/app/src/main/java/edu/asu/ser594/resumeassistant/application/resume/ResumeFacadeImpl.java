package edu.asu.ser594.resumeassistant.application.resume;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.application.resume.command.CreateVersionCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeEditCommand;
import edu.asu.ser594.resumeassistant.application.resume.command.ResumeUploadCommand;
import edu.asu.ser594.resumeassistant.application.resume.dto.ResumeDownloadResult;
import edu.asu.ser594.resumeassistant.application.resume.query.ResumeDownloadQuery;
import edu.asu.ser594.resumeassistant.application.resume.service.ResumeApplicationService;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeGroup;
import edu.asu.ser594.resumeassistant.domain.resume.entity.ResumeVersion;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import edu.asu.ser594.resumeassistant.domain.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 简历门面实现
 * Resume Facade Implementation
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeFacadeImpl implements ResumeFacade {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_TYPES = {
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/markdown",
            "text/plain"
    };
    private final ResumeApplicationService applicationService;

    @Override
    public ResumeUploadResponse uploadResume(ResumeUploadRequest request, UUID userId) {
        validateFile(request.getFile());

        try {
            ResumeUploadCommand command = ResumeUploadCommand.builder()
                    .fileName(request.getFile().getOriginalFilename())
                    .contentType(request.getFile().getContentType())
                    .fileSize(request.getFile().getSize())
                    .inputStream(request.getFile().getInputStream())
                    .title(request.getTitle())
                    .build();

            ResumeGroup group = applicationService.handleUpload(command, userId);

            // 获取原始版本ID
            // Get the original version ID
            ResumeVersion originalVersion = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);
            UUID originalVersionId = originalVersion != null ? originalVersion.getId() : null;

            return ResumeUploadResponse.builder()
                    .groupId(group.getId())
                    .originalVersionId(originalVersionId)
                    .title(group.getTitle())
                    .createdAt(group.getCreatedAt())
                    .build();

        } catch (IOException e) {
            throw new StorageException("resume.upload.io.error", e);
        }
    }

    @Override
    public ApiResponse<List<ResumeGroupResponse>> getResumeGroups(UUID userId) {
        List<ResumeGroup> groups = applicationService.listUserGroups(userId);
        return ApiResponse.success(groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList()));
    }

    @Override
    public ApiResponse<ResumeGroupResponse> getResumeGroup(UUID groupId, UUID userId) {
        ResumeGroup group = applicationService.getGroup(groupId, userId);
        return ApiResponse.success(toGroupResponse(group));
    }

    @Override
    public ApiResponse<List<ResumeVersionResponse>> getVersionsByGroup(UUID groupId, UUID userId) {
        ResumeGroup group = applicationService.getGroup(groupId, userId);
        return ApiResponse.success(group.getVersions().stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList()));
    }

    @Override
    public ApiResponse<ResumeVersionResponse> getVersion(UUID versionId, UUID userId) {
        ResumeVersion version = applicationService.getVersion(versionId, userId);
        return ApiResponse.success(toVersionResponse(version));
    }

    @Override
    public ApiResponse<ResumeVersionResponse> editVersion(ResumeEditRequest request, UUID userId) {
        ResumeEditCommand command = ResumeEditCommand.builder()
                .versionId(request.getVersionId())
                .userId(userId)
                .content(request.getContent())
                .build();

        ResumeVersion updated = applicationService.handleEdit(command);
        return ApiResponse.success(toVersionResponse(updated));
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadResume(UUID versionId, UUID userId,
                                                              String format) {
        ResumeDownloadQuery query = ResumeDownloadQuery.builder()
                .versionId(versionId)
                .userId(userId)
                .targetFormat(format)
                .build();

        ResumeDownloadResult result = applicationService.handleDownload(query);

        String encodedFileName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(new InputStreamResource(result.inputStream()));
    }

    @Override
    public ApiResponse<Void> deleteResumeGroup(UUID groupId, UUID userId) {
        applicationService.handleDelete(groupId, userId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Void> deleteVersion(UUID versionId, UUID userId) {
        applicationService.handleDeleteVersion(versionId, userId);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<ResumeGroupResponse> setDefaultGroup(UUID groupId, UUID userId) {
        throw new UnsupportedOperationException("MVP not implemented");
    }

    @Override
    public ApiResponse<ResumeVersionResponse> createAiVersion(UUID groupId, UUID userId) {
        throw new UnsupportedOperationException("MVP not implemented");
    }

    @Override
    public ApiResponse<ResumeVersionResponse> rollbackToVersion(UUID versionId, UUID userId) {
        throw new UnsupportedOperationException("MVP not implemented");
    }

    @Override
    public ApiResponse<ResumeVersionResponse> createVersion(UUID groupId,
                                                            edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest request,
                                                            UUID userId) {
        CreateVersionCommand command = CreateVersionCommand.builder()
                .groupId(groupId)
                .sourceVersionId(request.sourceVersionId())
                .userId(userId)
                .build();

        ResumeVersion newVersion = applicationService.handleCreateVersion(command);
        return ApiResponse.success(toVersionResponse(newVersion));
    }

    @Override
    public void handleParseResult(AiResultEvent event) {
        log.info("Handling parse result for ResumeVersion: {}", event.referenceId());
        applicationService.handleParseResult(event);
    }

    // ==================== 映射方法 ====================
    // ==================== Mapping method ====================

    private ResumeGroupResponse toGroupResponse(ResumeGroup group) {
        ResumeVersion original = group.getActiveVersionByType(ResumeVersion.VersionType.ORIGINAL);
        ResumeVersion converted = group.getActiveVersionByType(ResumeVersion.VersionType.CONVERTED);
        ResumeVersion ai = group.getActiveVersionByType(ResumeVersion.VersionType.AI_OPTIMIZED);

        return ResumeGroupResponse.builder()
                .groupId(group.getId())
                .title(group.getTitle())
                .isDefault(group.isDefault())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .originalVersion(toVersionSummary(original))
                .convertedVersion(toVersionSummary(converted))
                .aiOptimizedVersion(toVersionSummary(ai))
                .build();
    }

    private ResumeGroupResponse.VersionSummary toVersionSummary(ResumeVersion v) {
        if (v == null) return null;
        return ResumeGroupResponse.VersionSummary.builder()
                .versionId(v.getId())
                .status(v.getStatus().name())
                .createdAt(v.getCreatedAt())
                .exists(true)
                .build();
    }

    private ResumeVersionResponse toVersionResponse(ResumeVersion v) {
        return ResumeVersionResponse.builder()
                .versionId(v.getId())
                .groupId(v.getGroupId())
                .versionType(v.getVersionType().name())
                .status(v.getStatus().name())
                .originalFileName(v.getOriginalFileName())
                .fileType(v.getFileType())
                .fileSize(v.getFileSize())
                .content(v.getContent())
                .editable(v.isEditable())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("validation.file.required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException("resume.upload.size.exceeded");
        }
        boolean allowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(file.getContentType())) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new StorageException("resume.upload.type.invalid");
        }
    }
}
