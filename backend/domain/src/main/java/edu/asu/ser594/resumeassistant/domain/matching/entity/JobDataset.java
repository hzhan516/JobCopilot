package edu.asu.ser594.resumeassistant.domain.matching.entity;

import edu.asu.ser594.resumeassistant.domain.shared.entity.AggregateRoot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 职位数据集实体
 * Job dataset entity
 * <p>
 * 对应 job_dataset 表 / Corresponds to job_dataset table
 */
@Getter
public class JobDataset extends AggregateRoot<Long> {

    private final Long id;
    private final LocalDateTime createdAt;
    private String externalId;
    private String title;
    private String company;
    private String description;
    private List<String> requirements;
    private String location;
    private String experienceLevel;
    private String source;
    private Map<String, Object> rawData;

    @Builder
    public JobDataset(final Long id,
                      final String externalId,
                      final String title,
                      final String company,
                      final String description,
                      final List<String> requirements,
                      final String location,
                      final String experienceLevel,
                      final String source,
                      final Map<String, Object> rawData,
                      final LocalDateTime createdAt) {
        this.id = id;
        this.externalId = externalId;
        this.title = title;
        this.company = company;
        this.description = description;
        this.requirements = requirements;
        this.location = location;
        this.experienceLevel = experienceLevel;
        this.source = source;
        this.rawData = rawData;
        this.createdAt = createdAt;
    }

    @Override
    public Long getId() {
        return id;
    }
}
