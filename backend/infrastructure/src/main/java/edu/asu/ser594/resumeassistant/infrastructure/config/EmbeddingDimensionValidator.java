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
 * 嵌入向量维度启动校验器
 * Embedding dimension startup validator
 * <p>
 * 应用启动时比对 PostgreSQL 向量列的实际维度与配置维度是否一致。
 * Compares the actual PostgreSQL vector column dimension with the configured dimension on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingDimensionValidator implements ApplicationRunner {

    private final EmbeddingProperties embeddingProperties;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int expectedDim = embeddingProperties.getDimension();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查 resume_vectors.embedding 维度 / Check resume_vectors.embedding dimension
            checkColumnDimension(stmt, "resume_vectors", "embedding", expectedDim);
            // 检查 job_vectors.embedding 维度 / Check job_vectors.embedding dimension
            checkColumnDimension(stmt, "job_vectors", "embedding", expectedDim);

        } catch (Exception e) {
            log.warn("无法执行嵌入向量维度校验（数据库可能尚未初始化）: {}", e.getMessage());
            log.warn("Could not perform embedding dimension validation (database may not be initialized yet): {}", e.getMessage());
        }
    }

    private void checkColumnDimension(Statement stmt, String table, String column, int expectedDim) throws Exception {
        String sql = String.format(
                "SELECT atttypmod FROM pg_attribute "
                        + "WHERE attrelid = '%s'::regclass AND attname = '%s'",
                table, column
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
                                    + "2) 生产环境：手动更新 ResumeVectorJpaEntity / JobVectorJpaEntity 中的 columnDefinition，\n"
                                    + "   并重建向量表（所有现有向量将失效需重新生成）。\n"
                                    + "========================================",
                            table, column, actualDim, expectedDim,
                            table, column, actualDim, expectedDim
                    );
                } else if (actualDim > 0) {
                    log.info("向量列 {}.{} 维度校验通过: {}", table, column, actualDim);
                }
            }
        }
    }
}
