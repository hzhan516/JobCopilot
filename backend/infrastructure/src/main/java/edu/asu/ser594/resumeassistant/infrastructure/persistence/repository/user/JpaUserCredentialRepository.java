package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserCredentialJpaEntity;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// 用户凭证 JPA 仓储
// User credential JPA repository
@Repository
public interface JpaUserCredentialRepository extends JpaRepository<UserCredentialJpaEntity, UUID> {

    Optional<UserCredentialJpaEntity> findByUserIdAndCredentialType(UUID userId, CredentialType credentialType);
}