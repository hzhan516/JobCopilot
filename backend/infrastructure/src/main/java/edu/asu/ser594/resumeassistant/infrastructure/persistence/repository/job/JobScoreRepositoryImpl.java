package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.job;

import edu.asu.ser594.resumeassistant.domain.job.entity.JobScoreRecord;
import edu.asu.ser594.resumeassistant.domain.job.repository.JobScoreRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.job.JobScoreRecordJpaEntity;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.job.JobScoreRecordPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位评分记录仓库实现 / Job score record repository implementation
 */
@Repository
@RequiredArgsConstructor
public class JobScoreRepositoryImpl implements JobScoreRepository {

    private final JpaJobScoreRepository jpaJobScoreRepository;
    private final JobScoreRecordPersistenceMapper mapper;

    @Override
    public JobScoreRecord save(JobScoreRecord record) {
        JobScoreRecordJpaEntity entity = mapper.toEntity(record);
        JobScoreRecordJpaEntity saved = jpaJobScoreRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<JobScoreRecord> findAllByJobIdOrderByCreatedAtDesc(String jobId) {
        return jpaJobScoreRepository.findAllByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobScoreRecord> findAllByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaJobScoreRepository.findAllByUserIdOrderByCreatedAtDesc(userId.toString()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<JobScoreRecord> findLatestByJobIdAndResumeVersionId(String jobId, String resumeVersionId) {
        return jpaJobScoreRepository.findAllByJobIdAndResumeVersionIdOrderByCreatedAtDesc(jobId, resumeVersionId)
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }
}
