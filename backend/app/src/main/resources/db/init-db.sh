#!/bin/bash
set -e

# Resume Assistant - PostgreSQL initialization wrapper
# 智能求职助手 - PostgreSQL 初始化包装脚本
#
# This script reads LLM_EMBEDDING_MODEL_DIMENSION from the environment
# and substitutes the vector dimension in the SQL template before execution.
# 本脚本读取环境变量 LLM_EMBEDDING_MODEL_DIMENSION，
# 并在执行前替换 SQL 模板中的向量维度。

DIM=${LLM_EMBEDDING_MODEL_DIMENSION:-1536}
TEMPLATE="/docker-entrypoint-initdb.d/templates/init.sql"

if [ ! -f "$TEMPLATE" ]; then
    echo "ERROR: SQL template not found at $TEMPLATE"
    exit 1
fi

echo "============================================================"
echo "Resume Assistant Database Initialization"
echo "Embedding dimension from env: ${DIM}"
echo "============================================================"

# 替换向量维度 / Substitute vector dimension
PROCESSED=$(mktemp)
sed "s/vector(1536)/vector(${DIM})/g" "$TEMPLATE" > "$PROCESSED"

# 验证替换是否成功 / Verify substitution succeeded
# 先删除 SQL 单行注释，避免注释中的 vector(1536) 干扰验证
# Remove SQL single-line comments first to avoid false positives from comments
if [ "$DIM" != "1536" ] && sed 's/--.*$//' "$PROCESSED" | grep -q 'vector(1536)'; then
    echo "ERROR: Dimension substitution failed — vector(1536) still present in processed SQL."
    echo "错误：维度替换失败——处理后 SQL 中仍然存在 vector(1536)。"
    echo "Please ensure init.sql uses single-line 'vector(1536)' format."
    echo "请确保 init.sql 使用单行的 'vector(1536)' 格式。"
    rm -f "$PROCESSED"
    exit 1
fi

# 统计替换次数 / Count substitutions
MATCHES=$(grep -c "vector(${DIM})" "$PROCESSED" || true)
echo "Substitution verified: found ${MATCHES} occurrence(s) of vector(${DIM})."
echo "替换验证通过：找到 ${MATCHES} 处 vector(${DIM})。"

# 执行初始化脚本 / Execute initialization script
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" < "$PROCESSED"

rm -f "$PROCESSED"

echo "============================================================"
echo "Database initialization completed with dimension: ${DIM}"
echo "数据库初始化完成，使用维度: ${DIM}"
echo "============================================================"

# 条件创建 HNSW 索引（仅当维度不超过 pgvector 限制时）
# Conditionally create HNSW index only when within pgvector limit
if [ "$DIM" -le 2000 ]; then
    echo "Creating HNSW index for ${DIM}-dimensional vectors..."
    echo "正在为 ${DIM} 维向量创建 HNSW 索引..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOF
CREATE INDEX IF NOT EXISTS idx_job_vectors_embedding_hnsw ON job_vectors
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
EOF
    echo "HNSW index created."
    echo "HNSW 索引创建完成。"
else
    echo "WARNING: Dimension ${DIM} exceeds pgvector HNSW limit (2000). Skipping HNSW index."
    echo "警告：维度 ${DIM} 超过 pgvector HNSW 索引限制（2000），跳过 HNSW 索引创建。"
    echo "Vector search will use exact scan (slower but accurate for dev)."
    echo "向量搜索将使用精确扫描（开发环境较慢但结果准确）。"
fi
