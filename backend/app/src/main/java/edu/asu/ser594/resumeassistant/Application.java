package edu.asu.ser594.resumeassistant;

import edu.asu.ser594.resumeassistant.infrastructure.config.InfrastructureConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能求职助手后端服务启动类
 *
 * @author SER594 Team
 * @date 2024
 */
@SpringBootApplication(scanBasePackages = "edu.asu.ser594.resumeassistant")
@Import(InfrastructureConfig.class)
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
