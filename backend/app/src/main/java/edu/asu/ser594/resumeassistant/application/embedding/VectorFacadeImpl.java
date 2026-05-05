package edu.asu.ser594.resumeassistant.application.embedding;

import edu.asu.ser594.resumeassistant.api.embedding.facade.VectorFacade;
import edu.asu.ser594.resumeassistant.application.embedding.service.VectorApplicationService;
import edu.asu.ser594.resumeassistant.domain.shared.event.ai.AiResultEvent;
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
    public void handleVectorGenResult(AiResultEvent event) {
        vectorApplicationService.handleVectorGenResult(event);
    }
}
