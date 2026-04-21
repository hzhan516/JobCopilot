package edu.asu.ser594.resumeassistant.infrastructure.storage.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 * MinIO configuration - only activated when storage.type=minio
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "minio", matchIfMissing = true)
public class MinioConfig {

    private final StorageProperties storageProperties;

    @Bean
    public MinioClient minioClient() {
        StorageProperties.Minio minio = storageProperties.getMinio();
        log.info("Initializing MinIO client with endpoint: {}", minio.getEndpoint());
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
