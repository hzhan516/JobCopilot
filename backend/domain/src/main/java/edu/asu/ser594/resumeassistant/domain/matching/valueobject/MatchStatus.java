package edu.asu.ser594.resumeassistant.domain.matching.valueobject;

/**
 * 职位匹配结果状态枚举
 * Job match result status enumeration
 */
public enum MatchStatus {
    /**
     * 处理中 / Processing
     */
    PROCESSING,

    /**
     * 已完成 / Completed
     */
    COMPLETED,

    /**
     * 失败 / Failed
     */
    FAILED
}
