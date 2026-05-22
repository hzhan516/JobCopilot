package io.jobcopilot.resumeassistant.config;

import io.jobcopilot.resumeassistant.domain.matching.repository.MatchingModelRepository;
import io.jobcopilot.resumeassistant.domain.matching.service.ModelManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public ModelManagementService modelManagementService(MatchingModelRepository matchingModelRepository) {
        return new ModelManagementService(matchingModelRepository);
    }
}
