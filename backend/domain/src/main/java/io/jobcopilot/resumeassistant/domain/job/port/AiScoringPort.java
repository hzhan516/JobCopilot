package io.jobcopilot.resumeassistant.domain.job.port;

import java.util.Map;

/**
 * Port for delegating job-to-resume suitability scoring to an external AI service.
 * Implementations are provided in the infrastructure layer (e.g., REST adapter).
 * 将职位与简历适配度评分委托给外部 AI 服务的端口。实现由基础设施层提供（如 REST 适配器）。
 */
public interface AiScoringPort {

    /**
     * Invokes the AI service to compute a suitability score.
     * 调用 AI 服务计算适配度分数。
     *
     * @param jobId           Job ID / 职位 ID
     * @param resumeVersionId Resume version ID / 简历版本 ID
     * @param resume          Structured resume data / 结构化简历数据
     * @param job             Structured job data / 结构化职位数据
     * @param semanticMatch   Optional pre-computed semantic similarity / 可选的预计算语义相似度
     * @return AI response body as a map / AI 响应体 Map
     */
    Map<String, Object> score(String jobId, String resumeVersionId,
                              Map<String, Object> resume, Map<String, Object> job,
                              Float semanticMatch);
}
