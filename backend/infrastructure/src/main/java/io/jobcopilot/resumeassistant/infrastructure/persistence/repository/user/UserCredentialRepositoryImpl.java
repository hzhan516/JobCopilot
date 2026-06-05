package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.user;

import io.jobcopilot.resumeassistant.domain.user.entity.UserCredential;
import io.jobcopilot.resumeassistant.domain.user.repository.UserCredentialRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.UserCredentialPersistenceMapper;
import io.jobcopilot.resumeassistant.types.enums.CredentialType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserCredentialRepositoryImpl implements UserCredentialRepository {

    private final JpaUserCredentialRepository jpaRepository;
    private final UserCredentialPersistenceMapper mapper;

    @Override
    public UserCredential save(UserCredential credential) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(credential)));
    }

    @Override
    public Optional<UserCredential> findByUserIdAndType(UUID userId, CredentialType type) {
        return jpaRepository.findByUserIdAndCredentialType(userId, type)
                .map(mapper::toDomain);
    }
}