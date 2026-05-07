package edu.asu.ser594.resumeassistant.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import edu.asu.ser594.resumeassistant.infrastructure.rest.InternalApiKeyInterceptor;

/**
 * Infrastructure 模块配置
 * 显式扫描 infrastructure 包下的所有 Spring 组件
 */
@Configuration
@ComponentScan(basePackages = "edu.asu.ser594.resumeassistant.infrastructure")
public class InfrastructureConfig {

    @Bean
    public RestTemplate restTemplate(InternalApiKeyInterceptor internalApiKeyInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(internalApiKeyInterceptor);
        return restTemplate;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
