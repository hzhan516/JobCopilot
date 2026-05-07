package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserOAuthBinding;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserOAuthBindingJpaEntity;
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
