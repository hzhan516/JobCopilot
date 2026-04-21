package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.matching;

import edu.asu.ser594.resumeassistant.domain.matching.entity.JobMatchResult;
import edu.asu.ser594.resumeassistant.domain.matching.repository.JobMatchResultRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.matching.JobMatchResultPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 职位匹配结果仓储实现
 * Job match result repository implementation
 */
@Repository
@RequiredArgsConstructor
public class JobMatchResultRepositoryImpl implements JobMatchResultRepository {

    private final JpaJobMatchResultRepository jpaRepository;
    private final JobMatchResultPersistenceMapper mapper;

    @Override
    public JobMatchResult save(final JobMatchResult result) {
        var entity = mapper.toEntity(result);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<JobMatchResult> findById(final String matchId) {
        return jpaRepository.findById(matchId).map(mapper::toDomain);
    }

    @Override
    public List<JobMatchResult> findAllByUserId(final UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId.toString())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobMatchResult> findAllByUserIdOrderByCreatedAtDesc(final UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId.toString())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
