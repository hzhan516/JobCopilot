package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserCredentialRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.UserCredentialPersistenceMapper;
import edu.asu.ser594.resumeassistant.types.enums.CredentialType;
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