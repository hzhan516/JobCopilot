package edu.asu.ser594.resumeassistant.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure 模块配置
 * 显式扫描 infrastructure 包下的所有 Spring 组件
 */
@Configuration
@ComponentScan(basePackages = "edu.asu.ser594.resumeassistant.infrastructure")
public class InfrastructureConfig {
}
