package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import edu.asu.ser594.resumeassistant.types.enums.UserRole;
import edu.asu.ser594.resumeassistant.types.enums.UserStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends AggregateRoot<UUID> {

    private final UUID id;
    private final String email;
    private boolean emailVerified;
    private UserRole role;
    private UserStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static User create(String email) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return new User(
                id,
                email,
                false,
                UserRole.JOB_SEEKER,
                UserStatus.ACTIVE,
                now,
                now
        );
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public UUID getId() {
        return id;
    }
}