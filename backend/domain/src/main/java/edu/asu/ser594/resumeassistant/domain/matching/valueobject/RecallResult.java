package edu.asu.ser594.resumeassistant.domain.matching.valueobject;

/**
 * 召回结果项
 * Recall result item
 *
 * @param jobId 职位ID / Job ID
 * @param distance 向量距离 / Vector distance
 */
public record RecallResult(
        String jobId,
        Double distance
) {}
