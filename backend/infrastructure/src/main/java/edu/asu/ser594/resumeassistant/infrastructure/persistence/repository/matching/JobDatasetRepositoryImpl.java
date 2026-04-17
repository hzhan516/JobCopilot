package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.matching;

import edu.asu.ser594.resumeassistant.domain.matching.entity.JobDataset;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobDatasetRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.matching.JobDatasetPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 职位数据集仓储实现
 * Job dataset repository implementation
 */
@Repository
@RequiredArgsConstructor
public class JobDatasetRepositoryImpl implements JobDatasetRepository {

    private final JpaJobDatasetRepository jpaRepository;
    private final JobDatasetPersistenceMapper mapper;

    @Override
    public JobDataset save(final JobDataset dataset) {
        var entity = mapper.toEntity(dataset);
        if (dataset.getId() != null) {
            jpaRepository.findById(dataset.getId()).ifPresent(e -> entity.setId(e.getId()));
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<JobDataset> findById(final Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<JobDataset> findByExternalId(final String externalId) {
        return jpaRepository.findByExternalId(externalId).map(mapper::toDomain);
    }

    @Override
    public List<JobDataset> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
