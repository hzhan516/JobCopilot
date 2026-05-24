package io.jobcopilot.resumeassistant.application.config;

import io.jobcopilot.resumeassistant.domain.embedding.port.VectorGenerationPort;
import io.jobcopilot.resumeassistant.domain.resume.service.ResumeConverterService;
import io.jobcopilot.resumeassistant.domain.resume.service.VectorGenerationService;
import io.jobcopilot.resumeassistant.domain.shared.service.DocumentFormatConverter;
import io.jobcopilot.resumeassistant.domain.shared.service.FileStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Spring beans for domain-layer services that are plain Java classes.
 * This keeps the domain module free of Spring annotations while still allowing
 * dependency injection in the application layer.
 * 为纯 Java 的领域层服务提供 Spring Bean。使领域模块保持无 Spring 注解，
 * 同时仍允许在应用层进行依赖注入。
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public ResumeConverterService resumeConverterService(FileStorageService fileStorageService,
                                                         DocumentFormatConverter documentFormatConverter) {
        return new ResumeConverterService(fileStorageService, documentFormatConverter);
    }

    @Bean
    public VectorGenerationService vectorGenerationService(VectorGenerationPort vectorGenerationPort) {
        return new VectorGenerationService(vectorGenerationPort);
    }
}
