package edu.asu.ser594.resumeassistant.api.resume.facade;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeEditRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeGroupResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeVersionResponse;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * 简历门面接口
 * Resume facade interface
 */
public interface ResumeFacade {

    /**
     * 上传简历
     * Upload resume
     */
    ResumeUploadResponse uploadResume(ResumeUploadRequest request, UUID userId);

    /**
     * 获取用户的所有简历组
     * Get all resume groups of the user
     */
    ApiResponse<List<ResumeGroupResponse>> getResumeGroups(UUID userId);

    /**
     * 获取单个简历组详情
     * Get details of a single resume group
     */
    ApiResponse<ResumeGroupResponse> getResumeGroup(UUID groupId, UUID userId);

    /**
     * 获取简历组下的所有版本
     * Get all versions under the resume group
     */
    ApiResponse<List<ResumeVersionResponse>> getVersionsByGroup(UUID groupId, UUID userId);

    /**
     * 获取单个版本详情
     * Get details of a single version
     */
    ApiResponse<ResumeVersionResponse> getVersion(UUID versionId, UUID userId);

    /**
     * 编辑版本内容
     * Edit version content
     */
    ApiResponse<ResumeVersionResponse> editVersion(ResumeEditRequest request, UUID userId);

    /**
     * 下载简历
     * Download resume
     *
     * @param versionId 版本ID
     * @param userId    用户ID
     * @param format    导出格式（可选）：original, pdf, docx, html, md, txt
     * @return 文件流响应
     */
    ResponseEntity<InputStreamResource> downloadResume(UUID versionId, UUID userId, String format);

    /**
     * 删除简历组
     * Delete resume group
     */
    ApiResponse<Void> deleteResumeGroup(UUID groupId, UUID userId);

    /**
     * 删除简历版本
     * Delete resume version
     *
     * @param versionId 版本ID
     * @param userId    用户ID
     * @return 空响应
     */
    ApiResponse<Void> deleteVersion(UUID versionId, UUID userId);

    /**
     * 设置默认简历组
     * Set default resume group
     */
    ApiResponse<ResumeGroupResponse> setDefaultGroup(UUID groupId, UUID userId);

    /**
     * 创建AI优化版本
     * Create AI-optimized version
     */
    ApiResponse<ResumeVersionResponse> createAiVersion(UUID groupId, UUID userId);

    /**
     * 回滚到指定版本
     * Rollback to specified version
     */
    ApiResponse<ResumeVersionResponse> rollbackToVersion(UUID versionId, UUID userId);

    /**
     * 创建简历版本副本
     * Create resume version copy
     *
     * @param groupId 简历组ID / Resume group ID
     * @param request 创建请求 / Create request
     * @param userId  用户ID / User ID
     * @return 新版本详情 / New version details
     */
    ApiResponse<ResumeVersionResponse> createVersion(UUID groupId,
                                                     edu.asu.ser594.resumeassistant.api.resume.dto.request.CreateVersionRequest request,
                                                     UUID userId);

    /**
     * 处理AI解析结果的回调
     * Handle parse result from AI service
     */
    void handleParseResult(AiResultEvent event);
}
