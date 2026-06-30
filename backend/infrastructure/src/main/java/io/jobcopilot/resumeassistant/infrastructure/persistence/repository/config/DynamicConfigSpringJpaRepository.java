package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.config;

import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.config.DynamicConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface DynamicConfigSpringJpaRepository extends JpaRepository<DynamicConfigJpaEntity, String> {}
