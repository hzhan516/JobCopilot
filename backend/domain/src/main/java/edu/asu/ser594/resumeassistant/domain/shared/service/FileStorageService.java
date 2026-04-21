package edu.asu.ser594.resumeassistant.domain.shared.service;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

public interface FileStorageService {
    /**
     * 上传文件
     * Upload file
     */
    void upload(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * 下载文件
     * Download file
     */
    Optional<InputStream> download(String objectKey);

    /**
     * 删除文件
     * Delete file
     */
    void delete(String objectKey);

    /**
     * 检查文件是否存在
     * Check if file exists
     */
    boolean exists(String objectKey);

    /**
     * 生成预签名URL
     * Generate presigned URL
     */
    String generatePresignedUrl(String objectKey, Duration expiration);
}
