package io.jobcopilot.resumeassistant.domain.matching.repository;

import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;

import java.util.List;
import java.util.Optional;

/**
 * 职位数据集仓储接口
 * Repository interface for job datasets
 */
public interface JobDatasetRepository {

    /**
     * 保存数据集记录
     * Save a dataset record
     *
     * @param dataset 数据集实体 / Dataset entity
     * @return 保存后的实体 / Saved entity
     */
    JobDataset save(JobDataset dataset);

    /**
     * 根据ID查询
     * Find by ID
     *
     * @param id 记录ID / Record ID
     * @return 实体(可选) / Optional entity
     */
    Optional<JobDataset> findById(Long id);

    /**
     * 根据外部ID查询
     * Find by external ID
     *
     * @param externalId 外部ID / External ID
     * @return 实体(可选) / Optional entity
     */
    Optional<JobDataset> findByExternalId(String externalId);

    /**
     * 查询所有数据集
     * Find all datasets
     *
     * @return 数据集列表 / List of datasets
     */
    List<JobDataset> findAll();
}
