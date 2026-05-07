package edu.asu.ser594.resumeassistant.domain.matching.service;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.exception.MatchingException;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Enforces a singleton-active policy per model type (recall / ranker) to ensure consistent matching behavior.
 * 对每种模型类型（召回 / 精排）强制单激活策略，以保证匹配行为的一致性。
 */
@Slf4j
@RequiredArgsConstructor
public class ModelManagementService {

    private final MatchingModelRepository matchingModelRepository;

    public Optional<MatchingModel> getActiveRecallModel() {
        return matchingModelRepository.findActiveByType(ModelType.RECALL);
    }

    public Optional<MatchingModel> getActiveRankerModel() {
        return matchingModelRepository.findActiveByType(ModelType.RANKER);
    }

    /**
     * Atomically deactivates the currently active model of the same type before activating the target,
     * preventing multiple models from being active simultaneously.
     * 先原子性地停用同类型当前激活模型，再激活目标模型，防止多模型同时生效。
     *
     * @param modelId 要激活的模型ID | ID of the model to activate
     */
    public void switchActiveModel(final Long modelId) {
        MatchingModel target = matchingModelRepository.findById(modelId)
                .orElseThrow(() -> new MatchingException("matching.not.found"));

        Optional<MatchingModel> currentActiveOpt = matchingModelRepository.findActiveByType(target.getType());
        if (currentActiveOpt.isPresent()) {
            MatchingModel currentActive = currentActiveOpt.get();
            if (currentActive.getId().equals(modelId)) {
                return;
            }
            currentActive.deactivate();
            matchingModelRepository.save(currentActive);
            log.info("Deactivated previous model: {} version {}",
                    currentActive.getModelName(), currentActive.getVersion());
        }

        target.activate();
        matchingModelRepository.save(target);
        log.info("Activated model: {} version {}, type: {}",
                target.getModelName(), target.getVersion(), target.getType());
    }
}
