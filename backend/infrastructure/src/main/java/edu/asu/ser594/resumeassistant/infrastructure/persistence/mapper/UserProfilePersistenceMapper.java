package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserProfileJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfilePersistenceMapper {

    UserProfile toDomain(UserProfileJpaEntity entity);

    UserProfileJpaEntity toJpaEntity(UserProfile profile);
}
