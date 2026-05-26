package io.jobcopilot.resumeassistant.infrastructure.search;

import io.jobcopilot.resumeassistant.domain.matching.valueobject.RecallResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PGVector 向量搜索服务测试 / PGVector search service tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PGVector Search Service Tests / PGVector 搜索服务测试")
class PgVectorSearchServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private PgVectorSearchService searchService;

    @Test
    @DisplayName("Should find similar jobs by vector / 应通过向量搜索相似职位")
    void findSimilarJobs_ShouldReturnRecallResults() {
        // Given
        float[] resumeVector = new float[]{0.1f, 0.2f, 0.3f};
        int topK = 5;
        String modelVersion = "v1";

        Object[] row1 = new Object[]{"job-1", 0.25};
        Object[] row2 = new Object[]{"job-2", 0.35};
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("queryVector"), anyString())).thenReturn(query);
        when(query.setParameter(eq("topK"), eq(topK))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row1, row2));

        // When
        List<RecallResult> results = searchService.findSimilarJobs(resumeVector, topK, modelVersion);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).jobId()).isEqualTo("job-1");
        assertThat(results.get(0).distance()).isEqualTo(0.25);
        assertThat(results.get(1).jobId()).isEqualTo("job-2");
        assertThat(results.get(1).distance()).isEqualTo(0.35);
    }

    @Test
    @DisplayName("Should return empty list when no matching jobs / 当无匹配职位时应返回空列表")
    void findSimilarJobs_WhenNoResults_ShouldReturnEmptyList() {
        // Given
        float[] resumeVector = new float[]{0.1f, 0.2f};
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("queryVector"), anyString())).thenReturn(query);
        when(query.setParameter(eq("topK"), eq(3))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        // When
        List<RecallResult> results = searchService.findSimilarJobs(resumeVector, 3, "v1");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should set correct vector parameter / 应设置正确的向量参数")
    void findSimilarJobs_ShouldSetCorrectVectorParameter() {
        // Given
        float[] resumeVector = new float[]{1.0f, 2.0f};
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("queryVector"), anyString())).thenReturn(query);
        when(query.setParameter(eq("topK"), eq(10))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        // When
        searchService.findSimilarJobs(resumeVector, 10, "v1");

        // Then
        verify(query).setParameter("queryVector", "[1.0,2.0]");
        verify(query).setParameter("topK", 10);
    }

    @Test
    @DisplayName("Should build correct pg vector literal / 应构建正确的 PGVector 字面量")
    void findSimilarJobs_ShouldBuildCorrectPgVectorLiteral() {
        // Given
        float[] resumeVector = new float[]{0.5f, 1.5f, 2.5f};
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(eq("queryVector"), anyString())).thenReturn(query);
        when(query.setParameter(eq("topK"), eq(5))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        // When
        searchService.findSimilarJobs(resumeVector, 5, "v1");

        // Then
        verify(query).setParameter("queryVector", "[0.5,1.5,2.5]");
    }
}
