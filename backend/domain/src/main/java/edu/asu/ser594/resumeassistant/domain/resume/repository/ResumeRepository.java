package edu.asu.ser594.resumeassistant.domain.resume.repository;

import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * 简历仓储接口
 * Resume repository interface
 */
public interface ResumeRepository {

    Resume save(Resume resume);

    Resume updateStoragePath(UUID resumeId, String storagePath);

    Optional<Resume> findById(UUID id);

    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);

    Page<Resume> findByUserId(UUID userId, Pageable pageable);

    void delete(Resume resume);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
