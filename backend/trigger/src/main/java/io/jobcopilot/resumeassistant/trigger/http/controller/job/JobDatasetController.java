package io.jobcopilot.resumeassistant.trigger.http.controller.job;

import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobDatasetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 职位数据集内部 API / Job dataset internal API
 * <p>
 * 仅供 AI Service 调用，用于增量模型训练时的历史数据重建。
 * Only callable by AI Service for historical data rebuild during incremental model training.
 */
@Slf4j
@RestController
@RequestMapping("/v1/job-dataset")
@RequiredArgsConstructor
public class JobDatasetController {

    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-API-Key";
    private static final String ENV_INTERNAL_API_KEY = "INTERNAL_API_KEY";

    private final JobDatasetRepository jobDatasetRepository;

    /**
     * 查询全部职位数据集记录 / Query all job dataset records
     *
     * @param internalApiKey Internal API key header / 内部 API Key
     * @return List of job dataset records / 职位数据集记录列表
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAll(
            @RequestHeader(value = HEADER_INTERNAL_API_KEY, required = false) String internalApiKey
    ) {
        String expectedKey = System.getenv(ENV_INTERNAL_API_KEY);
        if (expectedKey != null && !expectedKey.isBlank()) {
            if (!expectedKey.equals(internalApiKey)) {
                log.warn("Unauthorized access to /v1/job-dataset: invalid or missing internal API key");
                return ResponseEntity.status(401).build();
            }
        }

        List<JobDataset> datasets = jobDatasetRepository.findAll();
        List<Map<String, Object>> results = datasets.stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "externalId", d.getExternalId(),
                        "title", d.getTitle(),
                        "company", d.getCompany(),
                        "description", d.getDescription(),
                        "requirements", d.getRequirements(),
                        "location", d.getLocation(),
                        "experienceLevel", d.getExperienceLevel(),
                        "source", d.getSource(),
                        "createdAt", d.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(results);
    }
}
