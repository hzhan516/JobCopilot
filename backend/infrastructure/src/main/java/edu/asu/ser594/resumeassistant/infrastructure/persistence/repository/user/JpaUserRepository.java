package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// User JPA repository
@Repository
public interface JpaUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}