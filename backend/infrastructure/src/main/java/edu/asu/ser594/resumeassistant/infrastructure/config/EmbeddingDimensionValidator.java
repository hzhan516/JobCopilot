package edu.asu.ser594.resumeassistant.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Validates that the PostgreSQL vector column dimension matches the application configuration at startup.
 * Mismatches silently break semantic search, so early detection prevents runtime failures.
 * 启动时校验 PostgreSQL 向量列维度与配置一致，防止维度不匹配导致语义搜索失效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingDimensionValidator implements ApplicationRunner {

    private final EmbeddingProperties embeddingProperties;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        int expectedDim = embeddingProperties.getDimension();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            checkColumnDimension(stmt, "resume_vectors", expectedDim);
            checkColumnDimension(stmt, "job_vectors", expectedDim);

        } catch (Exception e) {
            log.warn("无法执行嵌入向量维度校验（数据库可能尚未初始化）: {}", e.getMessage());
            log.warn("Could not perform embedding dimension validation (database may not be initialized yet): {}", e.getMessage());
        }
    }

    private void checkColumnDimension(Statement stmt, String table, int expectedDim) throws Exception {
        String sql = String.format(
                "SELECT atttypmod FROM pg_attribute "
                        + "WHERE attrelid = '%s'::regclass AND attname = '%s'",
                table, "embedding"
        );

        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int actualDim = rs.getInt("atttypmod");
                if (actualDim > 0 && actualDim != expectedDim) {
                    log.error(
                            "========================================\n"
                                    + "数据库表 {}.{} 的向量维度为 {}，但配置维度为 {}。\n"
                                    + "Database table {}.{} vector dimension is {}, but configured dimension is {}.\n"
                                    + "如需调整维度，请：\n"
                                    + "1) 开发环境：删除 postgres-data 卷后重新启动容器；\n"
                                    + "2) 生产环境：手动更新 ResumeVectorJpaEntity / JobVectorJpaEntity 中的 @Array(length = ...)，\n"
                                    + "   并重建向量表（所有现有向量将失效需重新生成）。\n"
                                    + "========================================",
                            table, "embedding", actualDim, expectedDim,
                            table, "embedding", actualDim, expectedDim
                    );
                } else if (actualDim > 0) {
                    log.info("向量列 {}.{} 维度校验通过: {}", table, "embedding", actualDim);
                }
            }
        }
    }
}
