package edu.asu.ser594.resumeassistant.api.job.dto.response;

import java.util.List;

/**
 * 职位的输出表示
 * Output representation of a Job.
 *
 * @param id                唯一职位标识符
 * @param id                The unique job identifier.
 * @param userId            提交职位的用户 ID
 * @param userId            The ID of the user who submitted the job.
 * @param originalUrl       职位发布页面的 URL
 * @param originalUrl       The URL of the job posting.
 * @param status            职位的当前处理状态
 * @param status            The current processing status of the job.
 * @param parsedContent     解析后的结构化数据（如果已完成）
 * @param parsedContent     The parsed structured data, if completed.
 * @param imageCheckEnabled 是否启用了视觉验证
 * @param imageCheckEnabled Whether visual verification was enabled.
 * @param errorMessage      如果职位处理失败时的错误信息
 * @param errorMessage      Any error message if the job failed.
 */
public record JobResponse(
        String id,
        String userId,
        String originalUrl,
        String status,
        ParsedJobContentResponse parsedContent,
        boolean imageCheckEnabled,
        String errorMessage
) {
    /**
     * 解析后职位内容的 DTO，以避免直接暴露领域值对象
     * DTO for parsed job content to avoid exposing domain value objects directly.
     *
     * @param title        职位标题
     * @param title        The job title.
     * @param company      招聘公司
     * @param company      The hiring company.
     * @param description  完整职位描述
     * @param description  The full job description.
     * @param requirements 要求或资格条件列表
     * @param requirements A list of requirements or qualifications.
     */
    public record ParsedJobContentResponse(
            String title,
            String company,
            String salary,
            String location,
            String description,
            List<String> requirements
    ) {
    }
}
