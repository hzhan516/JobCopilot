package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper;

import io.jobcopilot.resumeassistant.domain.user.entity.UserOAuthBinding;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserOAuthBindingJpaEntity;
import org.mapstruct.Mapper;

/**
 * OAuth 绑定持久化 Mapper
 * OAuth binding persistence mapper
 */
@Mapper(componentModel = "spring")
public interface UserOAuthBindingPersistenceMapper {

    UserOAuthBinding toDomain(UserOAuthBindingJpaEntity entity);

    UserOAuthBindingJpaEntity toJpaEntity(UserOAuthBinding binding);
}
