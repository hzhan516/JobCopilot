package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.user;

import io.jobcopilot.resumeassistant.domain.user.entity.User;
import io.jobcopilot.resumeassistant.domain.user.repository.UserRepository;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.user.UserJpaEntity;
import io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户仓库实现 / User repository implementation
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    /**
     * 保存用户 / Save user
     */
    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpaEntity(user);
        if (!jpaRepository.existsById(user.getId())) {
            entity.setVersion(null);
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    /**
     * 根据 ID 查询用户 / Find user by ID
     */
    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * 根据邮箱查询用户 / Find user by email
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    /**
     * 检查邮箱是否已存在 / Check if email exists
     */
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
