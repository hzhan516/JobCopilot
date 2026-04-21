package edu.asu.ser594.resumeassistant.domain.user.repository;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;

import java.util.Optional;
import java.util.UUID;

// User credential repository interface
public interface UserCredentialRepository {
    UserCredential save(UserCredential credential);

    Optional<UserCredential> findByUserIdAndType(UUID userId, CredentialType type);
}
