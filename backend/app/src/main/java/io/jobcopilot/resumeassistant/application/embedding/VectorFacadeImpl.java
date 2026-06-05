package io.jobcopilot.resumeassistant.application.embedding;

import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.application.embedding.service.VectorApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 向量门面实现 / Vector facade implementation
 * <p>
 * 将 API 层的入站请求委托给应用服务，保持门面薄层。
 * Thin facade that delegates inbound API requests to the application service.
 */
@Component
@RequiredArgsConstructor
public class VectorFacadeImpl implements VectorFacade {

    private final VectorApplicationService vectorApplicationService;

    @Override
    public void generateAndSaveVector(String referenceId, String entityType, String text) {
        vectorApplicationService.generateAndSaveVector(referenceId, entityType, text);
    }
}
