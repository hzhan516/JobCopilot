package edu.asu.ser594.resumeassistant.domain.matching.repository;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;

import java.util.List;
import java.util.Optional;

/**
 * 匹配模型仓储接口
 * Repository interface for matching models
 */
public interface MatchingModelRepository {

    /**
     * 保存模型
     * Save a model
     *
     * @param model 模型实体 / Model entity
     * @return 保存后的模型 / Saved model
     */
    MatchingModel save(MatchingModel model);

    /**
     * 根据ID查询模型
     * Find model by ID
     *
     * @param id 模型ID / Model ID
     * @return 模型实体(可选) / Optional model entity
     */
    Optional<MatchingModel> findById(Long id);

    /**
     * 获取指定类型的激活模型
     * Find active model by type
     *
     * @param type 模型类型 / Model type
     * @return 激活的模型(可选) / Optional active model
     */
    Optional<MatchingModel> findActiveByType(ModelType type);

    /**
     * 查询所有模型
     * Find all models
     *
     * @return 模型列表 / List of models
     */
    List<MatchingModel> findAll();

    /**
     * 根据类型查询所有模型
     * Find all models by type
     *
     * @param type 模型类型 / Model type
     * @return 模型列表 / List of models
     */
    List<MatchingModel> findAllByType(ModelType type);
}
