package edu.asu.ser594.resumeassistant.application.embedding.service;

import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchJobVectorUpsertRequest.JobVectorItem;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchJobVectorUpsertResponse;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.JobVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.JobVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 职位向量批量服务
 * Job vector batch service
 * <p>
 * 供 AI 层调用，批量 Upsert 职位向量数据。
 * 支持数据库级去重：内容完全相同的记录会被跳过，避免冗余写入。
 * Exposed for AI layer to batch upsert job vector data with deduplication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobVectorBatchService {

    private final JobVectorRepository jobVectorRepository;

    /**
     * 批量 Upsert 职位向量（带数据库去重）
     * Batch upsert job vectors with database deduplication
     *
     * @param items 职位向量条目列表 / List of job vector items
     * @return 批量操作结果 / Batch operation result
     */
    @Transactional
    public BatchJobVectorUpsertResponse batchUpsert(List<JobVectorItem> items) {
        if (items == null || items.isEmpty()) {
            return new BatchJobVectorUpsertResponse(0, 0, 0, 0, List.of());
        }

        log.info("Starting batch upsert for {} job vectors", items.size());
        List<JobVector> vectorsToSave = new ArrayList<>();
        List<String> failedJobIds = new ArrayList<>();
        int skipped = 0;

        for (JobVectorItem item : items) {
            try {
                if (item.jobId() == null || item.jobId().isBlank()) {
                    log.warn("Skipping item with blank jobId");
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
                JobVector vector = JobVector.createCompleted(
                        vectorId,
                        item.jobId(),
                        embedding,
                        item.title(),
                        item.description(),
                        item.requirements(),
                        item.rawContent(),
                        item.sourceFile(),
                        item.modelVersion()
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

    // 判断现有记录与新条目内容是否完全相同 / Check if existing record is identical to new item
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

    // 将 List<Float> 转为 float[] / Convert List<Float> to float[]
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
