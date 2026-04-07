package edu.asu.ser594.resumeassistant.api.resume.facade;

import edu.asu.ser594.resumeassistant.api.resume.dto.request.ResumeUploadRequest;
import edu.asu.ser594.resumeassistant.api.resume.dto.response.ResumeUploadResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

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
     * 下载简历
     * Download resume
     * 
     * @param resumeId 简历ID
     * @param userId 用户ID
     * @param exportFormat 导出格式（可选）：original, pdf, docx, html, md, txt
     * @return 文件流响应
     */
    ResponseEntity<InputStreamResource> downloadResume(UUID resumeId, UUID userId, String exportFormat);
}
