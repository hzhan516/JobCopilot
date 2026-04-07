package edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper;

import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume.ResumeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

/**
 * 简历持久化映射器
 * Resume persistence mapper
 */
@Mapper(componentModel = "spring")
@Component
public interface ResumePersistenceMapper {

    Resume toDomain(ResumeJpaEntity entity);

    ResumeJpaEntity toJpaEntity(Resume resume);

    /**
     * 更新已存在的实体对象（不创建新实例）
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDomain(Resume resume, @MappingTarget ResumeJpaEntity entity);
}
