package edu.asu.ser594.resumeassistant.trigger.http.controller.resume;

import edu.asu.ser594.resumeassistant.api.common.dto.ApiResponse;
import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import edu.asu.ser594.resumeassistant.api.resume.facade.ResumeFacade;
import edu.asu.ser594.resumeassistant.trigger.http.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(consumes = "multipart/form-data")
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
     * @param resumeId 简历ID
     * @param userId 当前用户ID
     * @param format 导出格式（可选）：original（默认）, pdf, docx, html, md, txt
     *               注意：当前版本仅返回原始文件，格式转换功能待实现
     */
    @GetMapping("/{resumeId}/download")
    public ResponseEntity<InputStreamResource> downloadResume(
            @PathVariable("resumeId") UUID resumeId,
            @CurrentUser UUID userId,
            @RequestParam(value = "format", required = false, defaultValue = "original") String format) {
        return resumeFacade.downloadResume(resumeId, userId, format);
    }
}
