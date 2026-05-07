package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper;

import edu.asu.ser594.resumeassistant.domain.user.entity.User;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    User toDomain(UserJpaEntity entity);

    UserJpaEntity toJpaEntity(User user);
}
