package io.jobcopilot.resumeassistant.application.embedding.service;

import io.jobcopilot.resumeassistant.api.embedding.dto.request.BatchResumeVectorUpsertRequest.ResumeVectorItem;
import io.jobcopilot.resumeassistant.api.embedding.dto.response.BatchResumeVectorUpsertResponse;
import io.jobcopilot.resumeassistant.domain.embedding.entity.ResumeVector;
import io.jobcopilot.resumeassistant.domain.embedding.repository.ResumeVectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Exposed to the AI layer for batch upserting resume vectors with embedding-level deduplication.
 * Skipping unchanged embeddings avoids unnecessary index churn in pgvector.
 * 向 AI 层暴露，支持带嵌入层级去重的简历向量批量 Upsert。跳过未变更的嵌入以避免 pgvector 不必要的索引扰动
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVectorBatchService {

    private final ResumeVectorRepository resumeVectorRepository;

    /**
     * Upserts resume vectors in batch. Compares embeddings byte-by-byte to skip records
     * that have not changed since the last ingestion cycle.
     * 批量 Upsert 简历向量。逐字节比对嵌入向量以跳过自上次摄入周期以来未发生变更的记录
     *
     * @param items List of resume vector items / 简历向量条目列表
     * @return Batch operation result / 批量操作结果
     */
    @Transactional(timeout = 30)
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
