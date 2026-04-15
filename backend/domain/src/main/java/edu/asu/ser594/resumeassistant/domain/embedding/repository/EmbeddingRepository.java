package edu.asu.ser594.resumeassistant.domain.embedding.repository;

import java.util.List;

public interface EmbeddingRepository {

    void saveResumeVector(String resumeVersionId, List<Double> embedding, String status, String errorMessage);

    void saveJobVector(String jobId, List<Double> embedding, String status, String errorMessage);
}
