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
import java.util.List;
import java.util.UUID;

/**
 * 职位向量批量服务
 * Job vector batch service
 * <p>
 * 供 AI 层调用，批量 Upsert 职位向量数据。
 * Exposed for AI layer to batch upsert job vector data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobVectorBatchService {

    private final JobVectorRepository jobVectorRepository;

    /**
     * 批量 Upsert 职位向量
     * Batch upsert job vectors
     *
     * @param items 职位向量条目列表 / List of job vector items
     * @return 批量操作结果 / Batch operation result
     */
    @Transactional
    public BatchJobVectorUpsertResponse batchUpsert(List<JobVectorItem> items) {
        if (items == null || items.isEmpty()) {
            return new BatchJobVectorUpsertResponse(0, 0, 0, List.of());
        }

        log.info("Starting batch upsert for {} job vectors", items.size());
        List<JobVector> vectors = new ArrayList<>();
        List<String> failedJobIds = new ArrayList<>();

        for (JobVectorItem item : items) {
            try {
                if (item.jobId() == null || item.jobId().isBlank()) {
                    log.warn("Skipping item with blank jobId");
                    continue;
                }
                float[] embedding = convertListToArray(item.embedding());
                JobVector vector = JobVector.createCompleted(
                        UUID.randomUUID().toString(),
                        item.jobId(),
                        embedding,
                        item.title(),
                        item.description(),
                        item.requirements(),
                        item.rawContent(),
                        item.sourceFile(),
                        item.modelVersion()
                );
                vectors.add(vector);
            } catch (Exception e) {
                log.warn("Failed to prepare vector for jobId: {}", item.jobId(), e);
                failedJobIds.add(item.jobId());
            }
        }

        if (!vectors.isEmpty()) {
            try {
                jobVectorRepository.saveAll(vectors);
                log.info("Successfully saved {} job vectors", vectors.size());
            } catch (Exception e) {
                log.error("Failed to save batch job vectors", e);
                // 将全部已解析的 jobId 标记为失败 / Mark all parsed jobIds as failed
                for (JobVector vector : vectors) {
                    failedJobIds.add(vector.getJobId());
                }
                vectors.clear();
            }
        }

        int success = vectors.size();
        int failed = items.size() - success;
        log.info("Batch upsert completed. Total: {}, Success: {}, Failed: {}", items.size(), success, failed);

        return new BatchJobVectorUpsertResponse(items.size(), success, failed, failedJobIds);
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
