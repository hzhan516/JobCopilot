package io.jobcopilot.resumeassistant.domain.matching.service;

import io.jobcopilot.resumeassistant.domain.matching.valueobject.RankedJob;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;

import java.util.List;

/**
 * 匹配策略接口
 * Matching strategy interface
 * <p>
 * 定义召回和精排的标准契约 / Defines standard contract for recall and ranking
 */
public interface MatchingStrategy {

    /**
     * 召回阶段：根据简历向量召回相似职位
     * Recall phase: retrieve similar jobs based on resume vector
     *
     * @param resumeVector 简历向量 / Resume vector
     * @param topK         召回数量 / Number of results to recall
     * @param modelVersion 模型版本 / Model version
     * @return 召回结果列表 / List of recall results
     */
    List<RecallResult> recall(float[] resumeVector, int topK, String modelVersion);

    /**
     * 精排阶段：对召回结果进行精细排序
     * Rank phase: perform fine-grained ranking on recalled results
     *
     * @param recalledJobs 召回的职位ID列表 / List of recalled job IDs
     * @param resumeText   简历文本 / Resume text
     * @param query        用户查询词 / User query
     * @return 精排结果列表 / List of ranked jobs
     */
    List<RankedJob> rank(List<String> recalledJobs, String resumeText, String query);
}
