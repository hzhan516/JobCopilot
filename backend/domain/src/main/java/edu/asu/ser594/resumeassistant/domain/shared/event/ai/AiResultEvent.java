package edu.asu.ser594.resumeassistant.domain.shared.event.ai;

import java.util.Map;

/**
 * AI结果事件
 * AI result event
 * 
 * @param referenceId 关联实体ID / Associated entity ID
 * @param type 任务类型 (e.g. RESUME_PARSE, JOB_PARSE, VECTOR_GEN) / Task type
 * @param status 状态 (COMPLETED, FAILED) / Status
 * @param data 解析数据 / Parsed data
 * @param errorMessage 错误信息 / Error message
 * @param eventType 内部事件路由类型 / Internal event routing type
 */
public record AiResultEvent(
    String referenceId,
    String type,
    String status,
    Map<String, Object> data,
    String errorMessage,
    String eventType
) {}

