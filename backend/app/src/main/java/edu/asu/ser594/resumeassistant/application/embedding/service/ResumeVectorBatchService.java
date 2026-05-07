package edu.asu.ser594.resumeassistant.application.embedding.service;

import edu.asu.ser594.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest.ResumeVectorItem;
import edu.asu.ser594.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import edu.asu.ser594.resumeassistant.domain.embedding.entity.ResumeVector;
import edu.asu.ser594.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 简历向量批量服务
 * Resume vector batch service
 * <p>
 * 供 AI 层调用，批量 Upsert 简历向量数据。
 * 支持数据库级去重：内容完全相同的记录会被跳过，避免冗余写入。
 * Exposed for AI layer to batch upsert resume vector data with deduplication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVectorBatchService {

    private final ResumeVectorRepository resumeVectorRepository;

    /**
     * 批量 Upsert 简历向量（带数据库去重）
     * Batch upsert resume vectors with database deduplication
     *
     * @param items 简历向量条目列表 / List of resume vector items
     * @return 批量操作结果 / Batch operation result
     */
    @Transactional
    public BatchResumeVectorUpsertResponse batchUpsert(List<ResumeVectorItem> items) {
        if (items == null || items.isEmpty()) {
            return new BatchResumeVectorUpsertResponse(0, 0, 0, 0, List.of());
        }

        log.info("Starting batch upsert for {} resume vectors", items.size());
        List<ResumeVector> vectorsToSave = new ArrayList<>();
        List<String> failedResumeVersionIds = new ArrayList<>();
        int skipped = 0;

        int index = 0;
        for (ResumeVectorItem item : items) {
            index++;
            try {
                if (item.resumeVersionId() == null || item.resumeVersionId().isBlank()) {
                    log.warn("Skipping item with blank resumeVersionId at index {}", index);
                    continue;
                }

                Optional<ResumeVector> existingOpt = resumeVectorRepository.findByResumeVersionId(item.resumeVersionId());
                if (existingOpt.isPresent()) {
                    float[] itemEmbedding = convertListToArray(item.embedding());
                    if (Arrays.equals(existingOpt.get().getEmbedding(), itemEmbedding)) {
                        log.debug("Content identical for resumeVersionId {}, skipping", item.resumeVersionId());
                        skipped++;
                        continue;
                    }
                }

                float[] embedding = convertListToArray(item.embedding());
                String vectorId = existingOpt.map(ResumeVector::getId).orElse(UUID.randomUUID().toString());
                ResumeVector vector = ResumeVector.createCompleted(vectorId, item.resumeVersionId(), embedding);
                vectorsToSave.add(vector);
            } catch (Exception e) {
                log.warn("Failed to prepare vector for resumeVersionId: {}", item.resumeVersionId(), e);
                failedResumeVersionIds.add(item.resumeVersionId());
            }
        }

        if (!vectorsToSave.isEmpty()) {
            try {
                resumeVectorRepository.saveAll(vectorsToSave);
                log.info("Successfully saved {} resume vectors", vectorsToSave.size());
            } catch (Exception e) {
                log.error("Failed to save batch resume vectors", e);
                for (ResumeVector vector : vectorsToSave) {
                    failedResumeVersionIds.add(vector.getResumeVersionId());
                }
                vectorsToSave.clear();
            }
        }

        int success = vectorsToSave.size();
        int failed = failedResumeVersionIds.size();
        log.info("Batch upsert completed. Total: {}, Success: {}, Skipped: {}, Failed: {}",
                items.size(), success, skipped, failed);

        return new BatchResumeVectorUpsertResponse(items.size(), success, failed, skipped, failedResumeVersionIds);
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
