package edu.asu.ser594.resumeassistant.domain.user.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.Entity;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserCredential implements Entity<UUID> {
    private final UUID id;
    private final UUID userId;
    private final CredentialType credentialType;
    private final String credentialValue;
    private final LocalDateTime lastChangedAt;
    private final LocalDateTime createdAt;

    public static UserCredential createPassword(UUID userId, String hashedPassword) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        return new UserCredential(
                id,
                userId,
                CredentialType.PASSWORD,
                hashedPassword,
                now,
                now
        );
    }

    @Override
    public UUID getId() {
        return id;
    }
}
