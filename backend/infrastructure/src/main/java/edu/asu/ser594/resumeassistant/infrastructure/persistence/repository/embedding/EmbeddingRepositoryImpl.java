package edu.asu.ser594.resumeassistant.infrastructure.persistence.repository.embedding;

import edu.asu.ser594.resumeassistant.domain.embedding.repository.EmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EmbeddingRepositoryImpl implements EmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void saveResumeVector(String resumeVersionId, List<Double> embedding, String status, String errorMessage) {
        String sql = "INSERT INTO resume_vectors (id, resume_version_id, embedding, status, error_message, created_at, updated_at) " +
                     "VALUES (?, ?, ?::vector, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (id) DO UPDATE SET " +
                     "embedding = EXCLUDED.embedding, " +
                     "status = EXCLUDED.status, " +
                     "error_message = EXCLUDED.error_message, " +
                     "updated_at = CURRENT_TIMESTAMP";
        
        String vectorString = embedding != null ? formatVector(embedding) : null;
        
        jdbcTemplate.update(sql,
                UUID.randomUUID().toString(),
                resumeVersionId,
                vectorString,
                status,
                errorMessage
        );
    }

    @Override
    public void saveJobVector(String jobId, List<Double> embedding, String status, String errorMessage) {
        String sql = "INSERT INTO job_vectors (id, job_id, embedding, status, error_message, created_at, updated_at) " +
                     "VALUES (?, ?, ?::vector, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (id) DO UPDATE SET " +
                     "embedding = EXCLUDED.embedding, " +
                     "status = EXCLUDED.status, " +
                     "error_message = EXCLUDED.error_message, " +
                     "updated_at = CURRENT_TIMESTAMP";

        String vectorString = embedding != null ? formatVector(embedding) : null;
        
        jdbcTemplate.update(sql,
                UUID.randomUUID().toString(),
                jobId,
                vectorString,
                status,
                errorMessage
        );
    }

    private String formatVector(List<Double> vector) {
        return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }
}
