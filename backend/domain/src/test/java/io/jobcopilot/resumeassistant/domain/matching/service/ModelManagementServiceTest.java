package io.jobcopilot.resumeassistant.domain.matching.service;

import io.jobcopilot.resumeassistant.domain.matching.entity.MatchingModel;
import io.jobcopilot.resumeassistant.domain.matching.exception.MatchingException;
import io.jobcopilot.resumeassistant.domain.matching.repository.MatchingModelRepository;
import io.jobcopilot.resumeassistant.domain.matching.valueobject.ModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 模型管理服务单元测试
 * Model management service unit tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Model Management Service Tests")
class ModelManagementServiceTest {

    @Mock
    private MatchingModelRepository matchingModelRepository;

    @InjectMocks
    private ModelManagementService modelManagementService;

    private MatchingModel recallModel;
    private MatchingModel rankerModel;

    @BeforeEach
    void setUp() {
        recallModel = MatchingModel.builder()
                .id(1L)
                .modelName("BERT-Recall")
                .version("v1.0")
                .type(ModelType.RECALL)
                .build();
        rankerModel = MatchingModel.builder()
                .id(2L)
                .modelName("GPT-Ranker")
                .version("v2.0")
                .type(ModelType.RANKER)
                .build();
    }

    @Test
    @DisplayName("Should get active recall model")
    void shouldGetActiveRecallModel() {
        when(matchingModelRepository.findActiveByType(ModelType.RECALL))
                .thenReturn(Optional.of(recallModel));

        Optional<MatchingModel> result = modelManagementService.getActiveRecallModel();

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(ModelType.RECALL);
    }

    @Test
    @DisplayName("Should get active ranker model")
    void shouldGetActiveRankerModel() {
        when(matchingModelRepository.findActiveByType(ModelType.RANKER))
                .thenReturn(Optional.of(rankerModel));

        Optional<MatchingModel> result = modelManagementService.getActiveRankerModel();

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(ModelType.RANKER);
    }

    @Test
    @DisplayName("Should return empty when no active model")
    void shouldReturnEmptyWhenNoActiveModel() {
        when(matchingModelRepository.findActiveByType(ModelType.RECALL))
                .thenReturn(Optional.empty());

        Optional<MatchingModel> result = modelManagementService.getActiveRecallModel();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should switch active model and deactivate previous")
    void shouldSwitchActiveModelAndDeactivatePrevious() {
        MatchingModel previousActive = MatchingModel.builder()
                .id(3L)
                .modelName("Old-Recall")
                .version("v0.9")
                .type(ModelType.RECALL)
                .build();
        previousActive.activate();

        when(matchingModelRepository.findById(1L)).thenReturn(Optional.of(recallModel));
        when(matchingModelRepository.findActiveByType(ModelType.RECALL))
                .thenReturn(Optional.of(previousActive));

        modelManagementService.switchActiveModel(1L);

        assertThat(previousActive.isActive()).isFalse();
        assertThat(recallModel.isActive()).isTrue();
        verify(matchingModelRepository).save(previousActive);
        verify(matchingModelRepository).save(recallModel);
    }

    @Test
    @DisplayName("Should not deactivate same model")
    void shouldNotDeactivateSameModel() {
        recallModel.activate();

        when(matchingModelRepository.findById(1L)).thenReturn(Optional.of(recallModel));
        when(matchingModelRepository.findActiveByType(ModelType.RECALL))
                .thenReturn(Optional.of(recallModel));

        modelManagementService.switchActiveModel(1L);

        assertThat(recallModel.isActive()).isTrue();
        verify(matchingModelRepository, never()).save(any(MatchingModel.class));
    }

    @Test
    @DisplayName("Should throw when model not found")
    void shouldThrowWhenModelNotFound() {
        when(matchingModelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelManagementService.switchActiveModel(99L))
                .isInstanceOf(MatchingException.class)
                .hasMessageContaining("matching.not.found");
    }
}
