package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

    Optional<UserProfileJpaEntity> findByUserId(UUID userId);
}
