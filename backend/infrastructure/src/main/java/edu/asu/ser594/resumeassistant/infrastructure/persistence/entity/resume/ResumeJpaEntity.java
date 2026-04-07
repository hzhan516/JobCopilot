package edu.asu.ser594.resumeassistant.infrastructure.persistence.entity.resume;

import edu.asu.ser594.resumeassistant.domain.resume.entity.Resume;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 简历JPA实体
 * Resume JPA entity
 */
@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ResumeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "storage_provider", nullable = false)
    private String storageProvider;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Resume.ProcessingStatus processingStatus;

    @Column(name = "parsed_content", columnDefinition = "TEXT")
    private String parsedContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}