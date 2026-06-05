package io.jobcopilot.resumeassistant.infrastructure.persistence.mapper;

import io.jobcopilot.resumeassistant.domain.user.entity.UserCredential;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserCredentialJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCredentialPersistenceMapper {

    UserCredential toDomain(UserCredentialJpaEntity entity);

    UserCredentialJpaEntity toJpaEntity(UserCredential credential);
}
