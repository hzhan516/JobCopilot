package io.jobcopilot.resumeassistant.application.shared.scheduler;

import io.jobcopilot.resumeassistant.domain.embedding.port.ModelRetrainingPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 增量模型重训练调度器测试 / Incremental model retraining scheduler tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Incremental Retraining Scheduler Tests / 增量模型重训练调度器测试")
class IncrementalRetrainingSchedulerTest {

    @Mock
    private ModelRetrainingPort modelRetrainingPort;

    @InjectMocks
    private IncrementalRetrainingScheduler scheduler;

    @Test
    @DisplayName("Should trigger incremental retraining via port / 应通过端口触发增量重训练")
    void triggerIncrementalRetraining_ShouldCallPort() {
        // When
        scheduler.triggerIncrementalRetraining();

        // Then
        verify(modelRetrainingPort).triggerRetraining();
    }

    @Test
    @DisplayName("Should swallow exception without throwing / 应吞掉异常而不抛出")
    void triggerIncrementalRetraining_WhenPortThrows_ShouldNotPropagate() {
        // Given
        doThrow(new RuntimeException("AI service unavailable"))
                .when(modelRetrainingPort).triggerRetraining();

        // When & Then — should not throw
        scheduler.triggerIncrementalRetraining();

        verify(modelRetrainingPort).triggerRetraining();
    }
}
