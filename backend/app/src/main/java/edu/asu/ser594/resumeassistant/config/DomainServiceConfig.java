package edu.asu.ser594.resumeassistant.config;

import edu.asu.ser594.resumeassistant.domain.matching.repository.MatchingModelRepository;
import edu.asu.ser594.resumeassistant.domain.matching.service.ModelManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public ModelManagementService modelManagementService(MatchingModelRepository matchingModelRepository) {
        return new ModelManagementService(matchingModelRepository);
    }
}
