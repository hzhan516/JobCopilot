package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.user;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserCredentialJpaEntity;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserCredentialRepository extends JpaRepository<UserCredentialJpaEntity, UUID> {

    Optional<UserCredentialJpaEntity> findByUserIdAndCredentialType(UUID userId, CredentialType credentialType);
}
