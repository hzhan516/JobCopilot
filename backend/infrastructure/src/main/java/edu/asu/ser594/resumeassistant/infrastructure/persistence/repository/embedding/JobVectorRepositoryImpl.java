package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import edu.asu.ser594.resumeassistant.domain.embedding.valueobject.JobVectorSearchResult;
import edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.embedding.JobVectorJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 职位向量仓库实现 / Job vector repository implementation
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobVectorRepositoryImpl implements JobVectorRepository {

    private final JobVectorJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    /**
     * 保存向量（存在则更新） / Save vector (update if exists)
     */
    @Override
    public void save(JobVector vector) {
        Optional<JobVectorJpaEntity> existing = jpaRepository.findByJobId(vector.getJobId());
        JobVectorJpaEntity entity = toEntity(vector);
        existing.ifPresent(e -> entity.setId(e.getId())); // 复用已有 id，JPA 就会执行 UPDATE / Reuse existing id for JPA UPDATE
        jpaRepository.save(entity);
    }

    /**
     * 批量保存向量（逐条判断存在性后保存） / Save vectors in batch (check existence per item)
     */
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

    /**
     * 根据职位 ID 查询向量 / Find vector by job ID
     */
    @Override
    public Optional<JobVector> findByJobId(String jobId) {
        return jpaRepository.findByJobId(jobId)
                .map(this::toDomain);
    }

    /**
     * 向量近似最近邻搜索 / Vector approximate nearest neighbor search
     */
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

    // 将 requirements 字段转为 JSON 字符串 / Convert requirements field to JSON string
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

    // 领域对象转 JPA 实体 / Domain object to JPA entity
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

    // JPA 实体转领域对象 / JPA entity to domain object
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
