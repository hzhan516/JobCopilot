package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.user;

import io.jobcopilot.resumeassistant.domain.user.entity.UserOAuthBinding;
import io.jobcopilot.resumeassistant.domain.user.repository.UserOAuthBindingRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.UserOAuthBindingPersistenceMapper;
import io.jobcopilot.resumeassistant.types.enums.OAuthProvider;
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
