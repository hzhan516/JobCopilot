package io.jobcopilot.resumeassistant.application.job.service;

import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.exception.JobException;
import io.jobcopilot.resumeassistant.domain.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * JobAccessControl 单元测试
 * 职位访问控制单元测试
 * <p>
 * 测试职位可见性与所有权验证规则：
 * Tests job visibility and ownership verification rules:
 * - 已删除职位不可见
 * - Deleted jobs are not accessible
 * - 越权访问拒绝
 * - Unauthorized access denial
 * - 不存在职位拒绝
 * - Non-existent job denial
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Access Control Tests")
class JobAccessControlTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobAccessControl accessControl;

    private UUID userId;
    private UUID otherUserId;
    private String jobId;
    private Job job;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        jobId = "job-123";

        job = Job.create(userId, "https://example.com/job", false);
    }

    // ==================== requireAccessible ====================

    @Test
    @DisplayName("Should return job when accessible by owner")
    void shouldReturnJobWhenAccessibleByOwner() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当 / When
        Job result = accessControl.requireAccessible(jobId, userId);

        // 那么 / Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw when job not found")
    void shouldThrowWhenJobNotFound() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireAccessible(jobId, userId))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("job.not.found");
    }

    @Test
    @DisplayName("Should throw when job is hidden (soft deleted)")
    void shouldThrowWhenJobIsHidden() {
        // 给定 / Given
        job.delete(); // soft delete
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireAccessible(jobId, userId))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("job.not.found");
    }

    @Test
    @DisplayName("Should throw access denied when job belongs to another user")
    void shouldThrowAccessDeniedWhenJobBelongsToAnotherUser() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireAccessible(jobId, otherUserId))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("access.denied");
    }

    // ==================== requireOwned ====================

    @Test
    @DisplayName("Should return job when owned even if hidden")
    void shouldReturnJobWhenOwnedEvenIfHidden() {
        // 给定 / Given
        job.delete(); // soft delete — but owner should still see it for admin purposes
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当 / When
        Job result = accessControl.requireOwned(jobId, userId);

        // 那么 / Then — owner can still "own" a hidden job
        assertThat(result).isNotNull();
        assertThat(result.isHidden()).isTrue();
    }

    @Test
    @DisplayName("Should throw job not found for owned check when job missing")
    void shouldThrowJobNotFoundForOwnedCheckWhenJobMissing() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireOwned(jobId, userId))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("job.not.found");
    }

    @Test
    @DisplayName("Should throw access denied for owned check when wrong user")
    void shouldThrowAccessDeniedForOwnedCheckWhenWrongUser() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当&那么 / When & Then
        assertThatThrownBy(() -> accessControl.requireOwned(jobId, otherUserId))
                .isInstanceOf(JobException.class)
                .hasMessageContaining("access.denied");
    }

    @Test
    @DisplayName("Should allow access for matching owner")
    void shouldAllowAccessForMatchingOwner() {
        // 给定 / Given
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        // 当 / When
        Job accessible = accessControl.requireAccessible(jobId, userId);
        Job owned = accessControl.requireOwned(jobId, userId);

        // 那么 / Then
        assertThat(accessible).isEqualTo(job);
        assertThat(owned).isEqualTo(job);
    }
}
