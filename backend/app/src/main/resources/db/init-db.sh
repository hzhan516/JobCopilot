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
if grep -q 'vector(1536)' "$PROCESSED"; then
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
