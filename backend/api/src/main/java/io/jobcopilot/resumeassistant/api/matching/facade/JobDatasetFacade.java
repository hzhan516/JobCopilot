package io.jobcopilot.resumeassistant.api.matching.facade;

import io.jobcopilot.resumeassistant.api.matching.dto.response.JobDatasetResponse;

import java.util.List;

/**
 * Facade for querying job dataset records (internal API for AI service).
 * 查询职位数据集记录的外观接口（供 AI 服务使用的内部 API）。
 */
public interface JobDatasetFacade {

    /**
     * List all job dataset records.
     * 查询全部职位数据集记录。
     */
    List<JobDatasetResponse> listAll();
}
