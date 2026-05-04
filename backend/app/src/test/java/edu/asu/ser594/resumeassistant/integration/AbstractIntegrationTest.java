package edu.asu.ser594.resumeassistant.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 集成测试基类 / Integration test base class
 * <p>
 * 提供 PostgreSQL + pgvector 的 Testcontainers 支持，
 * 替代 H2 内存数据库以兼容 PostgreSQL 特有的语法（vector、jsonb、TEXT[]、gen_random_uuid() 等）。
 * Provides PostgreSQL + pgvector Testcontainers support,
 * replacing H2 in-memory database for PostgreSQL-specific syntax compatibility.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    static {
        configureContainerRuntime();
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg15")
            .withDatabaseName("resume_assistant")
            .withUsername("resume_user")
            .withPassword("resume_pass");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    /**
     * 自动配置容器运行时（Docker 或 Podman）
     * Auto-configure container runtime (Docker or Podman)
     */
    private static void configureContainerRuntime() {
        // 如果 DOCKER_HOST 已设置或 Docker socket 存在，无需额外配置
        // If DOCKER_HOST is already set or Docker socket exists, no extra config needed
        if (System.getenv("DOCKER_HOST") != null || Files.exists(Paths.get("/var/run/docker.sock"))) {
            return;
        }

        // 尝试查找 Podman socket / Try to find Podman socket
        Path podmanSocket = findPodmanSocket();
        if (podmanSocket != null) {
            TestcontainersConfiguration.getInstance().getUserProperties()
                    .setProperty("docker.host", "unix://" + podmanSocket);
        }
    }

    private static Path findPodmanSocket() {
        // 常见的 Podman socket 路径 / Common Podman socket paths
        String[] candidates = {
                "/run/user/" + getUid() + "/podman/podman.sock",
                "/tmp/podman.sock",
                System.getProperty("user.home") + "/.local/share/containers/podman/machine/podman.sock"
        };

        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private static int getUid() {
        try {
            String userName = System.getProperty("user.name");
            Process process = new ProcessBuilder("id", "-u", userName).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            return Integer.parseInt(output);
        } catch (Exception e) {
            // 回退到解析 /proc/self/status / Fallback to parsing /proc/self/status
            try {
                String status = Files.readString(Paths.get("/proc/self/status"));
                for (String line : status.split("\n")) {
                    if (line.startsWith("Uid:")) {
                        return Integer.parseInt(line.split("\\s+")[1]);
                    }
                }
            } catch (Exception ignored) {
            }
            return 1000;
        }
    }
}
