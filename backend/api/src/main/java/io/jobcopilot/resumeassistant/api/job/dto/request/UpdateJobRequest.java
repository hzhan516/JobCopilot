package io.jobcopilot.resumeassistant.api.job.dto.request;

import java.util.List;

/**
 * 更新职位解析内容的请求
 * Request to update a job's parsed content.
 *
 * @param title        职位标题 / The job title.
 * @param company      招聘公司 / The hiring company.
 * @param salary       薪资范围 / The salary range.
 * @param location     工作地点 / The job location.
 * @param description  职位描述 / The job description.
 * @param requirements 职位要求列表 / A list of job requirements.
 */
public record UpdateJobRequest(
        String title,
        String company,
        String salary,
        String location,
        String description,
        List<String> requirements
) {
}
