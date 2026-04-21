package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.tracking;

import edu.asu.ser594.resumeassistant.domain.tracking.entity.ApplicationTracking;
import edu.asu.ser594.resumeassistant.domain.tracking.repository.ApplicationTrackingRepository;
import edu.asu.ser594.resumeassistant.domain.tracking.valueobject.ApplicationStatus;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.tracking.TrackingPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 求职申请跟踪仓储实现
 * Application tracking repository implementation
 */
@Repository
@RequiredArgsConstructor
public class ApplicationTrackingRepositoryImpl implements ApplicationTrackingRepository {

    private final JpaApplicationTrackingRepository jpaRepository;
    private final TrackingPersistenceMapper mapper;

    @Override
    public ApplicationTracking save(final ApplicationTracking tracking) {
        final var entity = mapper.toEntity(tracking);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<ApplicationTracking> findById(final String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ApplicationTracking> findByIdAndUserId(final String id, final UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId.toString()).map(mapper::toDomain);
    }

    @Override
    public List<ApplicationTracking> findAllByUserId(final UUID userId) {
        return jpaRepository.findAllByUserId(userId.toString())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationTracking> findAllByUserIdAndStatus(final UUID userId, final ApplicationStatus status) {
        return jpaRepository.findAllByUserIdAndStatus(userId.toString(), status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(final String id) {
        jpaRepository.deleteById(id);
    }
}
