package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserOAuthBindingJpaEntity;
import edu.asu.ser594.resumeassistant.types.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth 绑定 JPA 仓库
 * OAuth binding JPA repository
 */
@Repository
public interface UserOAuthBindingJpaRepository extends JpaRepository<UserOAuthBindingJpaEntity, UUID> {

    Optional<UserOAuthBindingJpaEntity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    List<UserOAuthBindingJpaEntity> findByUserId(UUID userId);
}
