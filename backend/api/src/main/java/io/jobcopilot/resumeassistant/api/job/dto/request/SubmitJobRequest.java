package io.jobcopilot.resumeassistant.api.job.dto.request;

/**
 * 提交新职位解析任务的请求
 * Request to submit a new job parsing task.
 *
 * @param url               职位发布页面的 URL
 * @param url               The URL of the job posting.
 * @param imageCheckEnabled 是否启用职位发布布局的视觉验证
 * @param imageCheckEnabled True to enable visual verification of the job posting layout.
 * @param screenshotBase64  用户上传的职位截图 Base64（用于 AI 解析 fallback）
 * @param screenshotBase64  User-uploaded job screenshot as Base64 (for AI parsing fallback).
 */
public record SubmitJobRequest(
        String url,
        boolean imageCheckEnabled,
        String screenshotBase64
) {
}
