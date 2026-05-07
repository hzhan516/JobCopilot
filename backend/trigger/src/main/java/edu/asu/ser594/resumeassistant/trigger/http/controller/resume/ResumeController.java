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
 * 简历控制器
 * Resume controller
 */
@RestController
@RequestMapping("/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeFacade resumeFacade;

    /**
     * 上传简历
     * Upload resume
     */
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
     * 下载简历
     * Download resume
     *
     * @param versionId 版本ID（originalVersionId）
     * @param userId    当前用户ID
     * @param format    导出格式（可选）：original（默认）, pdf, docx, html, md, txt
     *                  注意：当前版本仅返回原始文件，格式转换功能待实现
     */
    @GetMapping("/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId,
            @RequestParam(value = "format", required = false, defaultValue = "original") String format) {
        return resumeFacade.downloadResume(versionId, userId, format);
    }

    /**
     * 获取用户的所有简历组
     * Get all resume groups for current user
     *
     * @param userId 当前用户ID
     * @return 简历组列表
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<ResumeGroupResponse>>> getResumeGroups(
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getResumeGroups(userId));
    }

    /**
     * 获取简历组详情
     * Get resume group details
     *
     * @param groupId 简历组ID
     * @param userId  当前用户ID
     * @return 简历组详情
     */
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<ResumeGroupResponse>> getResumeGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getResumeGroup(groupId, userId));
    }

    /**
     * 删除简历组
     * Delete resume group
     *
     * @param groupId 简历组ID
     * @param userId  当前用户ID
     * @return 空响应
     */
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteResumeGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.deleteResumeGroup(groupId, userId));
    }

    /**
     * 获取简历组版本列表
     * Get resume versions by group
     *
     * @param groupId 简历组ID
     * @param userId  当前用户ID
     * @return 版本列表
     */
    @GetMapping("/groups/{groupId}/versions")
    public ResponseEntity<ApiResponse<List<ResumeVersionResponse>>> getVersionsByGroup(
            @PathVariable("groupId") UUID groupId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getVersionsByGroup(groupId, userId));
    }

    /**
     * 删除简历版本
     * Delete resume version
     *
     * @param versionId 版本ID
     * @param userId    当前用户ID
     * @return 空响应
     */
    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.deleteVersion(versionId, userId));
    }

    /**
     * 获取单个版本详情
     * Get single version details
     *
     * @param versionId 版本ID
     * @param userId    当前用户ID
     * @return 版本详情
     */
    @GetMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> getVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.getVersion(versionId, userId));
    }

    /**
     * 编辑版本内容
     * Edit version content
     *
     * @param versionId 版本ID
     * @param request   编辑请求
     * @param userId    当前用户ID
     * @return 更新后的版本详情
     */
    @PutMapping("/versions/{versionId}")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> editVersion(
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody ResumeEditRequest request,
            @CurrentUser UUID userId) {
        // 确保请求中的 versionId 与路径参数一致
        ResumeEditRequest updatedRequest = request.toBuilder()
                .versionId(versionId)
                .build();
        return ResponseEntity.ok(resumeFacade.editVersion(updatedRequest, userId));
    }

    /**
     * 创建简历版本副本
     * Create resume version copy
     *
     * @param groupId 简历组ID
     * @param request 创建请求（sourceVersionId 可选）
     * @param userId  当前用户ID
     * @return 新创建的版本详情
     */
    @PostMapping("/groups/{groupId}/versions")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> createVersion(
            @PathVariable("groupId") UUID groupId,
            @Valid @RequestBody CreateVersionRequest request,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.createVersion(groupId, request, userId));
    }

    /**
     * 激活简历版本
     * Activate resume version
     *
     * @param versionId 版本ID
     * @param userId    当前用户ID
     * @return 激活后的版本详情
     */
    @PostMapping("/versions/{versionId}/activate")
    public ResponseEntity<ApiResponse<ResumeVersionResponse>> activateVersion(
            @PathVariable("versionId") UUID versionId,
            @CurrentUser UUID userId) {
        return ResponseEntity.ok(resumeFacade.activateVersion(versionId, userId));
    }
}
