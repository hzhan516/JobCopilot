package edu.asu.ser594.resumeassistant.application.matching.service;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.service.ModelManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 模型管理应用服务
 * Model management application service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManagementApplicationService {

    private final ModelManagementService modelManagementService;

    /**
     * 获取激活的召回模型
     * Get active recall model
     *
     * @return 激活的召回模型(可选) / Optional active recall model
     */
    @Transactional(readOnly = true)
    public Optional<MatchingModel> getActiveRecallModel() {
        return modelManagementService.getActiveRecallModel();
    }

    /**
     * 获取激活的精排模型
     * Get active ranker model
     *
     * @return 激活的精排模型(可选) / Optional active ranker model
     */
    @Transactional(readOnly = true)
    public Optional<MatchingModel> getActiveRankerModel() {
        return modelManagementService.getActiveRankerModel();
    }

    /**
     * 切换激活模型
     * Switch active model
     *
     * @param modelId 模型ID / Model ID
     */
    @Transactional
    public void switchActiveModel(final Long modelId) {
        modelManagementService.switchActiveModel(modelId);
    }
}
