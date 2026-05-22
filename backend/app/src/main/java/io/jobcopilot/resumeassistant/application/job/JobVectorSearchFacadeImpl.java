package io.jobcopilot.resumeassistant.application.job;

import io.jobcopilot.resumeassistant.api.job.dto.request.VectorSearchRequest;
import io.jobcopilot.resumeassistant.api.job.dto.response.VectorSearchResponse;
import io.jobcopilot.resumeassistant.api.job.facade.JobVectorSearchFacade;
import io.jobcopilot.resumeassistant.application.embedding.service.JobVectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 职位向量搜索门面实现
 * Job vector search facade implementation
 */
@Component
@RequiredArgsConstructor
public class JobVectorSearchFacadeImpl implements JobVectorSearchFacade {

    private final JobVectorSearchService jobVectorSearchService;

    @Override
    public List<VectorSearchResponse> search(VectorSearchRequest request) {
        return jobVectorSearchService.search(request);
    }
}
