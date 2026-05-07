package edu.asu.ser594.resumeassistant.trigger.http.controller.resume;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for resume lifecycle management: upload, versioning, download, and grouping.
 * 简历生命周期管理的 REST 端点，涵盖上传、版本控制、下载与分组
 */
@RestController
@RequestMapping("/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeFacade resumeFacade;

    @PostMapping(value = "", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @CurrentUser UUID userId) {

        ResumeUploadRequest request = ResumeUploadRequest.builder()
                .file(file)
                .title(title)
                .build();

        ResumeUploadResponse response = resumeFacade.uploadResume(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Downloads the raw file by default; format conversion (pdf, docx, etc.) is reserved for future extension.
     * 默认下载原始文件；格式转换（pdf、docx 等）预留为未来扩展点
     *
     * @param versionId the original version identifier / 原始版本标识
     * @param format    desired export format, currently only "original" is supported / 期望的导出格式，当前仅支持 original
     */
    @GetMapping("/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId,
            @RequestParam(value = "format", required = false, defaultValue = "original") String format) {
        return resumeFacade.downloadResume(versionId, userId, format);
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> getResumeGroups(
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getResumeGroups(userId));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<ResumeGroupResponse>> getResumeGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getResumeGroup(groupId, userId));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteResumeGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.deleteResumeGroup(groupId, userId));
    }

    @GetMapping("/groups/{groupId}/versions")
    public ResponseEntity<ApiResponse<List<ResumeVersionResponse>>> getVersionsByGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getVersionsByGroup(groupId, userId));
    }

    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.deleteVersion(versionId, userId));
    }

    @GetMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> getVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getVersion(versionId, userId));
    }

    @PutMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> editVersion(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody ResumeEditRequest request,
            @CurrentUser UUID userId) {
        // enforce path-version consistency to prevent accidental cross-version edits | 强制路径与请求体版本一致，防止误操作跨版本编辑
        ResumeEditRequest updatedRequest = request.toBuilder()
                .versionId(versionId)
                .build();
        return ResponseEntity.ok(resumeFacade.editVersion(updatedRequest, userId));
    }

    @PostMapping("/groups/{groupId}/versions")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> createVersion(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody CreateVersionRequest request,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.createVersion(groupId, request, userId));
    }

    @PostMapping("/versions/{versionId}/activate")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> activateVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.activateVersion(versionId, userId));
    }
}
