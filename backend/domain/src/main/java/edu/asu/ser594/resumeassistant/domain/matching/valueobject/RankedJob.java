package edu.asu.ser594.resumeassistant.domain.matching.valueobject;

/**
 * 精排后的职位结果项
 * Ranked job result item
 *
 * @param jobId 职位ID / Job ID
 * @param title 职位标题 / Job title
 * @param company 公司名称 / Company name
 * @param matchScore 匹配得分 / Match score
 * @param description 职位描述 / Job description
 */
public record RankedJob(
        String jobId,
        String title,
        String company,
        Double matchScore,
        String description
) {}
