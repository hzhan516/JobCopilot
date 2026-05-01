package edu.asu.ser594.resumeassistant.domain.job.valueobject;

import java.util.List;

/**
 * 表示从职位发布中解析出的结构化数据
 * Represents the structured data parsed from a job posting.
 * 包含由LLM提取的核心信息
 * Contains core information extracted by the LLM.
 * 
 * @param title The job title.
 * @param company The hiring company.
 * @param description The full job description.
 * @param requirements A list of requirements or qualifications.
 */
public record ParsedJobContent(
    String title,
    String company,
    String description,
    List<String> requirements
) {
}
