package edu.asu.ser594.resumeassistant.domain.user.repository;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for credential storage, abstracting away the underlying authentication mechanism.
 * 凭证存储的仓储契约，对底层认证机制进行抽象。
 */
public interface UserCredentialRepository {
    UserCredential save(UserCredential credential);

    Optional<UserCredential> findByUserIdAndType(UUID userId, CredentialType type);
}
