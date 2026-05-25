package io.jobcopilot.resumeassistant.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import io.jobcopilot.resumeassistant.infrastructure.rest.InternalApiKeyInterceptor;
import java.time.Duration;

/**
 * Infrastructure 模块配置
 * 显式扫描 infrastructure 包下的所有 Spring 组件
 */
@Configuration
@ComponentScan(basePackages = "io.jobcopilot.resumeassistant.infrastructure")
public class InfrastructureConfig {

    @Bean
    public RestTemplate restTemplate(InternalApiKeyInterceptor internalApiKeyInterceptor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(internalApiKeyInterceptor);
        return restTemplate;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
