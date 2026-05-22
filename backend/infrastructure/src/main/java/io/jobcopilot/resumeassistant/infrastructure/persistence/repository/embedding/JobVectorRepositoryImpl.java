package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import io.jobcopilot.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;
import io.jobcopilot.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure implementation of JobVectorRepository bridging domain operations
 * to pgvector-backed JPA queries and JSON field handling.
 * JobVectorRepository 的基础设施实现，桥接领域操作与 pgvector 原生查询及 JSON 字段处理
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobVectorRepositoryImpl implements JobVectorRepository {

    private final JobVectorJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void save(JobVector vector) {
        Optional<JobVectorJpaEntity> existing = jpaRepository.findByJobId(vector.getJobId());
        JobVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId())); // Reuse ID to trigger JPA UPDATE | 复用已有 ID 使 JPA 执行更新
        jpaRepository.save(entity);
    }

    @Override
    public void saveAll(List<JobVector> vectors) {
        List<JobVectorJpaEntity> entities = new ArrayList<>(vectors.size());
        for (JobVector vector : vectors) {
            Optional<JobVectorJpaEntity> existing = jpaRepository.findByJobId(vector.getJobId());
            JobVectorJpaEntity entity = toEntity(vector);
            existing.ifPresent(e -> entity.setId(e.getId()));
            entities.add(entity);
        }
        jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<JobVector> findByJobId(String jobId) {
        return jpaRepository.findByJobId(jobId)
                .map(this::toDomain);
    }

    @Override
    public List<JobVectorSearchResult> findNearestNeighbors(String vectorStr, int limit) {
        List<Object[]> results = jpaRepository.findNearestNeighbors(vectorStr, limit);
        List<JobVectorSearchResult> searchResults = new ArrayList<>(results.size());
        for (Object[] row : results) {
            String jobId = (String) row[0];
            String title = (String) row[1];
            String description = (String) row[2];
            String requirementsJson = convertRequirementsToJson(row[3]);
            String rawContent = (String) row[4];
            Double similarity = ((Number) row[5]).doubleValue();
            searchResults.add(new JobVectorSearchResult(jobId, title, description, requirementsJson, rawContent, similarity));
        }
        return searchResults;
    }

    private String convertRequirementsToJson(Object requirementsObj) {
        if (requirementsObj == null) {
            return null;
        }
        if (requirementsObj instanceof String str) {
            return str;
        }
        try {
            return objectMapper.writeValueAsString(requirementsObj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize requirements to JSON", e);
            return null;
        }
    }

    private JobVectorJpaEntity toEntity(JobVector domain) {
        return JobVectorJpaEntity.builder()
                .id(domain.getId())
                .jobId(domain.getJobId())
                .embedding(domain.getEmbedding())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .requirements(domain.getRequirements())
                .rawContent(domain.getRawContent())
                .sourceFile(domain.getSourceFile())
                .modelVersion(domain.getModelVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private JobVector toDomain(JobVectorJpaEntity entity) {
        return new JobVector(
                entity.getId(),
                entity.getJobId(),
                entity.getEmbedding(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getRequirements(),
                entity.getRawContent(),
                entity.getSourceFile(),
                entity.getModelVersion()
        );
    }
}
