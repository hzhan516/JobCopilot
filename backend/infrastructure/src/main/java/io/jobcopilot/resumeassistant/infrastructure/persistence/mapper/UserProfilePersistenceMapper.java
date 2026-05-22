package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper;

import io.jobcopilot.resumeassistant.domain.user.entity.UserProfile;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserProfileJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfilePersistenceMapper {

    UserProfile toDomain(UserProfileJpaEntity entity);

    UserProfileJpaEntity toJpaEntity(UserProfile profile);
}
