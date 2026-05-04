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

echo "Initializing database with embedding dimension: ${DIM}"
sed "s/vector(1536)/vector(${DIM})/g" "$TEMPLATE" | \
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"

echo "Database initialization completed."
