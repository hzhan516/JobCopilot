package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserCredential;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.user.UserCredentialJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserCredentialPersistenceMapper {

    UserCredential toDomain(UserCredentialJpaEntity entity);

    UserCredentialJpaEntity toJpaEntity(UserCredential credential);
}
