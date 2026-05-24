package io.jobcopilot.resumeassistant.infrastructure.embedding.adapter;

import io.jobcopilot.resumeassistant.api.embedding.facade.VectorFacade;
import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter that bridges the domain {@link VectorGenerationPort} to the API-layer
 * {@link VectorFacade}. Keeps the domain layer free of API-module dependencies.
 * 将领域层 {@link VectorGenerationPort} 桥接到 API 层 {@link VectorFacade} 的适配器。
 * 确保领域层不依赖 API 模块。
 */
@Component
@RequiredArgsConstructor
public class VectorGenerationFacadeAdapter implements VectorGenerationPort {

    private final VectorFacade vectorFacade;

    @Override
    public void generateAndSaveVector(String referenceId, String entityType, String text) {
        vectorFacade.generateAndSaveVector(referenceId, entityType, text);
    }
}
