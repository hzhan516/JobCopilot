package edu.asu.ser594.resumeassistant.domain.matching.service;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 模型管理服务
 * Model management service
 *
 * 领域服务：负责模型激活、切换等核心业务逻辑
 * Domain service: responsible for model activation, switching, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelManagementService {

    private final MatchingModelRepository matchingModelRepository;

    /**
     * 获取当前激活的召回模型
     * Get currently active recall model
     *
     * @return 激活的召回模型(可选) / Optional active recall model
     */
    public Optional<MatchingModel> getActiveRecallModel() {
        return matchingModelRepository.findActiveByType(ModelType.RECALL);
    }

    /**
     * 获取当前激活的精排模型
     * Get currently active ranker model
     *
     * @return 激活的精排模型(可选) / Optional active ranker model
     */
    public Optional<MatchingModel> getActiveRankerModel() {
        return matchingModelRepository.findActiveByType(ModelType.RANKER);
    }

    /**
     * 切换激活的模型版本
     * Switch active model version
     *
     * @param modelId 要激活的模型ID / Model ID to activate
     */
    public void switchActiveModel(final Long modelId) {
        MatchingModel target = matchingModelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));

        matchingModelRepository.findActiveByType(target.getType())
                .ifPresent(currentActive -> {
                    if (!currentActive.getId().equals(modelId)) {
                        currentActive.deactivate();
                        matchingModelRepository.save(currentActive);
                        log.info("Deactivated previous model: {} version {}",
                                currentActive.getModelName(), currentActive.getVersion());
                    }
                });

        target.activate();
        matchingModelRepository.save(target);
        log.info("Activated model: {} version {}, type: {}",
                target.getModelName(), target.getVersion(), target.getType());
    }
}
