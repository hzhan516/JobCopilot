package edu.asu.ser594.resumeassistant.api.job.facade;

import edu.asu.ser594.resumeassistant.api.job.dto.request.VectorSearchRequest;
import edu.asu.ser594.resumeassistant.api.job.dto.response.VectorSearchResponse;

import java.util.List;

/**
 * 职位向量搜索门面接口
 * Job vector search facade interface
 */
public interface JobVectorSearchFacade {

    /**
     * 执行向量搜索
     * Execute vector search
     *
     * @param request 搜索请求 / Search request
     * @return 搜索结果列表 / List of search results
     */
    List<VectorSearchResponse> search(VectorSearchRequest request);
}
