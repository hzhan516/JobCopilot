package io.jobcopilot.resumeassistant.application.matching;

import io.jobcopilot.resumeassistant.api.matching.dto.response.JobDatasetResponse;
import io.jobcopilot.resumeassistant.api.matching.facade.JobDatasetFacade;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobDatasetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Anti-corruption layer that shields the HTTP trigger layer from domain job dataset concepts.
 * 防腐层，将领域层的职位数据集概念转换为 HTTP 触发层可消费的 DTO。
 */
@Component
@RequiredArgsConstructor
public class JobDatasetFacadeImpl implements JobDatasetFacade {

    private final JobDatasetRepository jobDatasetRepository;

    @Override
    public List<JobDatasetResponse> listAll() {
        List<JobDataset> datasets = jobDatasetRepository.findAll();
        return datasets.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private JobDatasetResponse mapToResponse(JobDataset d) {
        return new JobDatasetResponse(
                d.getId(),
                d.getExternalId(),
                d.getTitle(),
                d.getCompany(),
                d.getDescription(),
                formatRequirements(d.getRequirements()),
                d.getLocation(),
                d.getExperienceLevel(),
                d.getSource(),
                toInstant(d.getCreatedAt())
        );
    }

    private String formatRequirements(String[] requirements) {
        return requirements == null ? "" : String.join("\n", requirements);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
