package edu.asu.ser594.resumeassistant.domain.user.repository;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserOAuthBinding;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 绑定仓储接口
 * OAuth binding repository interface
 */
public interface UserOAuthBindingRepository {

    UserOAuthBinding save(UserOAuthBinding binding);

    Optional<UserOAuthBinding> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    List<UserOAuthBinding> findByUserId(UUID userId);
}
