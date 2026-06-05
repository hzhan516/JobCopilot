package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.config.EmbeddingConfig;
import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest.JobVectorItem;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import io.jobcopilot.resumeassistant.domain.embedding.entity.JobVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.JobVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Exposed to the AI layer for batch upserting job vectors with content-level deduplication.
 * Skipping identical records reduces write pressure on pgvector and avoids redundant index updates.
 * 向 AI 层暴露，支持带内容级去重的职位向量批量 Upsert。跳过相同记录可降低 pgvector 写入压力并避免冗余索引更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobVectorBatchService {

    private final JobVectorRepository jobVectorRepository;
    private final EmbeddingConfig embeddingConfig;

    /**
     * Upserts job vectors in batch. Existing records are compared field-by-field and skipped
     * when no change is detected, keeping the index stable across repeated ingestion runs.
     * 批量 Upsert 职位向量。对已有记录逐字段比对，无变更时跳过，保证重复摄入时索引稳定
     *
     * @param items List of job vector items / 职位向量条目列表
     * @return Batch operation result / 批量操作结果
     */
    @Transactional(timeout = 30)
    public BatchJobVectorUpsertResponse batchUpsert(List<JobVectorItem> items) {
        if (items == null || items.isEmpty()) {
            return new BatchJobVectorUpsertResponse(0, 0, 0, 0, List.of());
        }

        log.info("Starting batch upsert for {} job vectors", items.size());
        List<JobVector> vectorsToSave = new ArrayList<>();
        List<String> failedJobIds = new ArrayList<>();
        int skipped = 0;

        int index = 0;
        for (JobVectorItem item : items) {
            index++;
            try {
                if (item.jobId() == null || item.jobId().isBlank()) {
                    log.warn("Skipping item with blank jobId at index {}", index);
                    continue;
                }

                Optional<JobVector> existingOpt = jobVectorRepository.findByJobId(item.jobId());
                if (existingOpt.isPresent()) {
                    if (isContentIdentical(existingOpt.get(), item)) {
                        log.debug("Content identical for jobId {}, skipping", item.jobId());
                        skipped++;
                        continue;
                    }
                }

                float[] embedding = convertListToArray(item.embedding());
                String vectorId = existingOpt.map(JobVector::getId).orElse(UUID.randomUUID().toString());
                String modelVersion = item.modelVersion() != null
                        ? item.modelVersion()
                        : embeddingConfig.getDefaultModelVersion();
                JobVector vector = JobVector.createCompleted(
                        vectorId,
                        item.jobId(),
                        embedding,
                        item.title(),
                        item.description(),
                        item.requirements(),
                        item.rawContent(),
                        item.sourceFile(),
                        modelVersion
                );
                vectorsToSave.add(vector);
            } catch (Exception e) {
                log.warn("Failed to prepare vector for jobId: {}", item.jobId(), e);
                failedJobIds.add(item.jobId());
            }
        }

        if (!vectorsToSave.isEmpty()) {
            try {
                jobVectorRepository.saveAll(vectorsToSave);
                log.info("Successfully saved {} job vectors", vectorsToSave.size());
            } catch (Exception e) {
                log.error("Failed to save batch job vectors", e);
                for (JobVector vector : vectorsToSave) {
                    failedJobIds.add(vector.getJobId());
                }
                vectorsToSave.clear();
            }
        }

        int success = vectorsToSave.size();
        int failed = failedJobIds.size();
        log.info("Batch upsert completed. Total: {}, Success: {}, Skipped: {}, Failed: {}",
                items.size(), success, skipped, failed);

        return new BatchJobVectorUpsertResponse(items.size(), success, failed, skipped, failedJobIds);
    }

    private boolean isContentIdentical(JobVector existing, JobVectorItem item) {
        float[] itemEmbedding;
        try {
            itemEmbedding = convertListToArray(item.embedding());
        } catch (Exception e) {
            return false;
        }
        if (!Arrays.equals(existing.getEmbedding(), itemEmbedding)) {
            return false;
        }
        return Objects.equals(existing.getTitle(), item.title())
                && Objects.equals(existing.getDescription(), item.description())
                && Objects.equals(existing.getRequirements(), item.requirements())
                && Objects.equals(existing.getRawContent(), item.rawContent())
                && Objects.equals(existing.getSourceFile(), item.sourceFile())
                && Objects.equals(existing.getModelVersion(), item.modelVersion());
    }

    private float[] convertListToArray(List<Float> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Embedding list must not be null or empty");
        }
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
