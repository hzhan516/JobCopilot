package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.config;

import io.jobcopilot.resumeassistant.domain.config.entity.DynamicConfig;
import io.jobcopilot.resumeassistant.domain.config.repository.DynamicConfigRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.config.DynamicConfigJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DynamicConfigJpaRepository implements DynamicConfigRepository {

    private final DynamicConfigSpringJpaRepository springRepo;

    @Override
    public List<DynamicConfig> findAll() {
        return springRepo.findAll().stream().map(DynamicConfigJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<DynamicConfig> findByKey(String key) {
        return springRepo.findById(key).map(DynamicConfigJpaEntity::toDomain);
    }

    @Override
    public DynamicConfig save(DynamicConfig config) {
        return springRepo.save(DynamicConfigJpaEntity.fromDomain(config)).toDomain();
    }
}
