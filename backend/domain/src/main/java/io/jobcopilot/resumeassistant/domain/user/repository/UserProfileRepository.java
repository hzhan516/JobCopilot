package io.jobcopilot.resumeassistant.domain.user.repository;

import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findById(UUID id);

    Optional<UserProfile> findByUserId(UUID userId);
}
