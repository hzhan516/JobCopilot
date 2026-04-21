package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.matching;

import edu.asu.ser594.resumeassistant.domain.matching.entity.MatchingModel;
import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.matching.MatchingModelPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 匹配模型仓储实现
 * Matching model repository implementation
 */
@Repository
@RequiredArgsConstructor
public class MatchingModelRepositoryImpl implements MatchingModelRepository {

    private final JpaMatchingModelRepository jpaRepository;
    private final MatchingModelPersistenceMapper mapper;

    @Override
    public MatchingModel save(final MatchingModel model) {
        var entity = mapper.toEntity(model);
        if (model.getId() != null) {
            jpaRepository.findById(model.getId()).ifPresent(e -> entity.setId(e.getId()));
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<MatchingModel> findById(final Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MatchingModel> findActiveByType(final ModelType type) {
        return jpaRepository.findByTypeAndIsActiveTrue(type).map(mapper::toDomain);
    }

    @Override
    public List<MatchingModel> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MatchingModel> findAllByType(final ModelType type) {
        return jpaRepository.findAllByType(type).stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
