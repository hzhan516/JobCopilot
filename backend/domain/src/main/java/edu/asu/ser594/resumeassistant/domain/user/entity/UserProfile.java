package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class UserProfile implements Entity<UUID> {

    private final UUID id;
    private final UUID userId;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private String targetPosition;
    private String preferredLocation;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfile create(UUID userId) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return new UserProfile(
                id,
                userId,
                null,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}