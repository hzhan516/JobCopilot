package io.jobcopilot.resumeassistant.infrastructure.embedding.adapter;

import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * VectorGenerationFacadeAdapter 单元测试
 * 向量生成门面适配器单元测试
 * <p>
 * 测试领域端口到 API 门面的委托调用：
 * Tests the delegation from domain port to API facade:
 * - 参数透传
 * - Parameter pass-through
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vector Generation Facade Adapter Tests")
class VectorGenerationFacadeAdapterTest {

    @Mock
    private VectorFacade vectorFacade;

    @InjectMocks
    private VectorGenerationFacadeAdapter adapter;

    @Test
    @DisplayName("Should delegate generateAndSaveVector to facade")
    void shouldDelegateGenerateAndSaveVectorToFacade() {
        // 当 / When
        adapter.generateAndSaveVector("ref-123", "RESUME", "content text");

        // 那么 / Then
        verify(vectorFacade).generateAndSaveVector("ref-123", "RESUME", "content text");
    }

    @Test
    @DisplayName("Should delegate with JOB entity type")
    void shouldDelegateWithJobEntityType() {
        // 当 / When
        adapter.generateAndSaveVector("job-456", "JOB", "job description");

        // 那么 / Then
        verify(vectorFacade).generateAndSaveVector("job-456", "JOB", "job description");
    }
}
