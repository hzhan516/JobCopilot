package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserOAuthBinding;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserOAuthBindingRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.UserOAuthBindingPersistenceMapper;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 绑定仓库实现
 * OAuth binding repository implementation
 */
@Repository
@RequiredArgsConstructor
public class UserOAuthBindingRepositoryImpl implements UserOAuthBindingRepository {

    private final UserOAuthBindingJpaRepository jpaRepository;
    private final UserOAuthBindingPersistenceMapper mapper;

    @Override
    public UserOAuthBinding save(UserOAuthBinding binding) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(binding)));
    }

    @Override
    public Optional<UserOAuthBinding> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId) {
        return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId).map(mapper::toDomain);
    }

    @Override
    public List<UserOAuthBinding> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
