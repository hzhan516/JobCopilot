package io.jobcopilot.resumeassistant.application.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jobcopilot.resumeassistant.domain.job.entity.Job;
import io.jobcopilot.resumeassistant.domain.job.valueobject.ParsedJobContent;
import io.jobcopilot.resumeassistant.domain.matching.entity.JobDataset;
import io.jobcopilot.resumeassistant.domain.matching.repository.JobDatasetRepository;
import io.jobcopilot.resumeassistant.domain.shared.event.ai.AiResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Synchronizes successfully parsed jobs to the ML training dataset.
 * 将成功解析的职位同步到 ML 训练数据集。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDatasetSyncService {

    private final JobDatasetRepository jobDatasetRepository;

    public void sync(Job job, AiResultEvent event) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ParsedJobContent pc = job.getParsedContent();
            JobDataset dataset = JobDataset.builder()
                    .externalId(job.getId())
                    .title(pc.title())
                    .company(pc.company())
                    .description(pc.description())
                    .requirements(pc.requirements() != null ? pc.requirements().toArray(new String[0]) : new String[0])
                    .location(pc.location())
                    .experienceLevel(null)
                    .source("USER_SUBMITTED")
                    .rawData(mapper.convertValue(event.data(), Map.class))
                    .build();
            jobDatasetRepository.save(dataset);
            log.info("Job {} saved to training dataset", job.getId());
        } catch (Exception e) {
            log.error("Failed to save job to dataset: {}", job.getId(), e);
        }
    }

    public ParsedJobContent mapToParsedContent(Map<String, Object> data) {
        return new ObjectMapper().convertValue(data, ParsedJobContent.class);
    }

    public String buildVectorText(ParsedJobContent pc) {
        return pc.title() + "\n" + pc.company() + "\n" + pc.description() + "\n"
                + String.join("\n", pc.requirements());
    }
}
