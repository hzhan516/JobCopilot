package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.job;

import edu.asu.ser594.resumeassistant.domain.job.entity.Job;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job.JobJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.job.JobPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepository {

    private final JpaJobRepository jpaJobRepository;
    private final JobPersistenceMapper jobMapper;

    @Override
    public Job save(Job job) {
        JobJpaEntity entity = jobMapper.toEntity(job);
        JobJpaEntity savedEntity = jpaJobRepository.save(entity);
        return jobMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Job> findById(String id) {
        return jpaJobRepository.findById(id).map(jobMapper::toDomain);
    }

    @Override
    public List<Job> findAllByUserId(String userId) {
        return jpaJobRepository.findAllByUserId(userId).stream()
                .map(jobMapper::toDomain)
                .collect(Collectors.toList());
    }
}

