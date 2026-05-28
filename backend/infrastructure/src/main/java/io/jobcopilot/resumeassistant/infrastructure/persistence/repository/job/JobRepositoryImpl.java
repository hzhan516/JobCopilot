package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.job;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.job.JobPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位仓库实现 / Job repository implementation
 */
@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepository {

    private final JpaJobRepository jpaJobRepository;
    private final JobPersistenceMapper jobMapper;

    /**
     * 保存职位 / Save job
     */
    @Override
    public void save(Job job) {
        JobJpaEntity entity = jobMapper.toEntity(job);
        if (!jpaJobRepository.existsById(job.getId())) {
            entity.setVersion(null);
        }
        jpaJobRepository.save(entity);
    }

    /**
     * 根据 ID 查询职位 / Find job by ID
     */
    @Override
    public Optional<Job> findById(String id) {
        return jpaJobRepository.findById(id).map(jobMapper::toDomain);
    }

    /**
     * 根据用户 ID 查询所有职位 / Find all jobs by user ID
     */
    @Override
    public List<Job> findAllByUserId(UUID userId) {
        return jpaJobRepository.findAllByUserIdAndHiddenAtIsNull(userId.toString()).stream()
                .map(jobMapper::toDomain)
                .collect(Collectors.toList());
    }
}
