package edu.asu.ser594.resumeassistant.domain.user.repository;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findById(UUID id);

    Optional<UserProfile> findByUserId(UUID userId);
}
