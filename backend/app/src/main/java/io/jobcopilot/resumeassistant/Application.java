package io.jobcopilot.resumeassistant;

import io.jobcopilot.resumeassistant.infrastructure.config.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能求职助手后端服务启动类
 * Intelligent job search assistant back-end service startup class
 *
 * @author JobCopilot Labs
 * @date 2024
 */
@SpringBootApplication(scanBasePackages = "io.jobcopilot.resumeassistant")
@Import(InfrastructureConfig.class)
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
