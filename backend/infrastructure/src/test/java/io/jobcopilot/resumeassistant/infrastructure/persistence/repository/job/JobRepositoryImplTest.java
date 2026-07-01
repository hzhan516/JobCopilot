package io.jobcopilot.resumeassistant.infrastructure.persistence.repository.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JobRepositoryImpl 单元测试
 * JobRepositoryImpl Unit Tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Repository Implementation Tests")
class JobRepositoryImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private JpaJobRepository jpaJobRepository;

    @Mock
    private io.jobcopilot.resumeassistant.infrastructure.persistence.mapper.job.JobPersistenceMapper jobMapper;

    @InjectMocks
    private JobRepositoryImpl jobRepository;

    @Test
    @DisplayName("Should count jobs by user ID converting UUID to string")
    void shouldCountJobsByUserId() {
        // Given
        when(jpaJobRepository.countByUserId(USER_ID.toString())).thenReturn(7L);

        // When
        long result = jobRepository.countByUserId(USER_ID);

        // Then
        assertThat(result).isEqualTo(7L);
        verify(jpaJobRepository).countByUserId(USER_ID.toString());
    }
}
