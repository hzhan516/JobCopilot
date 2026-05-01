package edu.asu.ser594.resumeassistant.api.job.dto.request;

/**
 * 提交新职位解析任务的请求
 * Request to submit a new job parsing task.
 *
 * @param url               职位发布页面的 URL
 * @param url               The URL of the job posting.
 * @param imageCheckEnabled 是否启用职位发布布局的视觉验证
 * @param imageCheckEnabled True to enable visual verification of the job posting layout.
 */
public record SubmitJobRequest(
        String url,
        boolean imageCheckEnabled
) {
}
