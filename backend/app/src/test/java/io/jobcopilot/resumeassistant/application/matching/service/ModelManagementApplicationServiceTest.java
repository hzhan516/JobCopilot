package io.jobcopilot.resumeassistant.application.matching.service;

import io.jobcopilot.resumeassistant.domain.matching.entity.MatchingModel;
import io.jobcopilot.resumeassistant.domain.matching.service.ModelManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 模型管理应用服务测试 / Model management application service tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Model Management Application Service Tests / 模型管理应用服务测试")
class ModelManagementApplicationServiceTest {

    @Mock
    private ModelManagementService modelManagementService;

    @InjectMocks
    private ModelManagementApplicationService applicationService;

    @Test
    @DisplayName("Should delegate get active recall model to domain service / 应将获取激活召回模型委托给领域服务")
    void getActiveRecallModel_ShouldDelegateToDomainService() {
        // Given
        MatchingModel expected = MatchingModel.builder().id(1L).modelName("recall-v1").build();
        when(modelManagementService.getActiveRecallModel()).thenReturn(Optional.of(expected));

        // When
        Optional<MatchingModel> result = applicationService.getActiveRecallModel();

        // Then
        assertThat(result).isPresent().hasValue(expected);
        verify(modelManagementService).getActiveRecallModel();
    }

    @Test
    @DisplayName("Should return empty when no active recall model / 当无激活召回模型时应返回空")
    void getActiveRecallModel_WhenNoneActive_ShouldReturnEmpty() {
        // Given
        when(modelManagementService.getActiveRecallModel()).thenReturn(Optional.empty());

        // When
        Optional<MatchingModel> result = applicationService.getActiveRecallModel();

        // Then
        assertThat(result).isEmpty();
        verify(modelManagementService).getActiveRecallModel();
    }

    @Test
    @DisplayName("Should delegate get active ranker model to domain service / 应将获取激活精排模型委托给领域服务")
    void getActiveRankerModel_ShouldDelegateToDomainService() {
        // Given
        MatchingModel expected = MatchingModel.builder().id(2L).modelName("ranker-v1").build();
        when(modelManagementService.getActiveRankerModel()).thenReturn(Optional.of(expected));

        // When
        Optional<MatchingModel> result = applicationService.getActiveRankerModel();

        // Then
        assertThat(result).isPresent().hasValue(expected);
        verify(modelManagementService).getActiveRankerModel();
    }

    @Test
    @DisplayName("Should return empty when no active ranker model / 当无激活精排模型时应返回空")
    void getActiveRankerModel_WhenNoneActive_ShouldReturnEmpty() {
        // Given
        when(modelManagementService.getActiveRankerModel()).thenReturn(Optional.empty());

        // When
        Optional<MatchingModel> result = applicationService.getActiveRankerModel();

        // Then
        assertThat(result).isEmpty();
        verify(modelManagementService).getActiveRankerModel();
    }

    @Test
    @DisplayName("Should delegate switch active model to domain service / 应将切换激活模型委托给领域服务")
    void switchActiveModel_ShouldDelegateToDomainService() {
        // Given
        Long modelId = 1L;

        // When
        applicationService.switchActiveModel(modelId);

        // Then
        verify(modelManagementService).switchActiveModel(modelId);
    }
}
