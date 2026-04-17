package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.matching;

import edu.asu.ser594.resumeassistant.domain.matching.valueobject.ModelType;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.matching.MatchingModelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 匹配模型 Spring Data JPA 接口
 * Matching model Spring Data JPA repository
 */
@Repository
public interface JpaMatchingModelRepository extends JpaRepository<MatchingModelJpaEntity, Long> {

    Optional<MatchingModelJpaEntity> findByTypeAndIsActiveTrue(ModelType type);

    List<MatchingModelJpaEntity> findAllByType(ModelType type);
}
