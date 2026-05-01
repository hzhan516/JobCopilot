package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.user;

import edu.asu.ser594.resumeassistant.domain.user.entity.UserProfile;
import edu.asu.ser594.resumeassistant.domain.user.repository.UserProfileRepository;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.mapper.UserProfilePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** 用户资料仓库实现 / User profile repository implementation */
@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final JpaUserProfileRepository jpaRepository;
    private final UserProfilePersistenceMapper mapper;

    /** 保存用户资料 / Save user profile */
    @Override
    public UserProfile save(UserProfile profile) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(profile)));
    }

    /** 根据 ID 查询用户资料 / Find user profile by ID */
    @Override
    public Optional<UserProfile> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /** 根据用户 ID 查询用户资料 / Find user profile by user ID */
    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }
}
