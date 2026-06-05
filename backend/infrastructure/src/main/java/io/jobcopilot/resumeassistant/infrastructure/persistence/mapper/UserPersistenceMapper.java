package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper;

import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    User toDomain(UserJpaEntity entity);

    UserJpaEntity toJpaEntity(User user);
}
