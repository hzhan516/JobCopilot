#!/bin/bash
# =============================================================================
# generate-secrets.sh — Generate Kubernetes Secrets from .env file
# generate-secrets.sh — 从 .env 文件生成 Kubernetes Secret
# =============================================================================
# Usage:
#   ./generate-secrets.sh /path/to/.env
#
# Outputs a Secret manifest to stdout. Pipe to kubectl apply -f - or save to file.
# 将 Secret 清单输出到标准输出。可管道到 kubectl apply -f - 或保存到文件。

set -e

ENV_FILE="${1:-.env}"
NAMESPACE="${2:-resume-assistant}"
SECRET_NAME="${3:-resume-assistant-secrets}"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: Environment file not found: $ENV_FILE"
    echo "Usage: $0 <path-to-.env> [namespace] [secret-name]"
    exit 1
fi

# Read values from .env / 从 .env 读取值
JWT_SECRET="${JWT_SECRET:-$(grep '^JWT_SECRET=' "$ENV_FILE" | cut -d= -f2- || true)}"
INTERNAL_API_KEY="${INTERNAL_API_KEY:-$(grep '^INTERNAL_API_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-$(grep '^POSTGRES_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-$(grep '^RABBITMQ_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)}"
REDIS_PASSWORD="${REDIS_PASSWORD:-$(grep '^REDIS_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)}"
GEMINI_API_KEY="${GEMINI_API_KEY:-$(grep '^GEMINI_API_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
OPENAI_API_KEY="${OPENAI_API_KEY:-$(grep '^OPENAI_API_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-$(grep '^ANTHROPIC_API_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
SMTP_PASSWORD="${SMTP_PASSWORD:-$(grep '^SMTP_PASSWORD=' "$ENV_FILE" | cut -d= -f2- || true)}"
AWS_S3_ACCESS_KEY="${AWS_S3_ACCESS_KEY:-$(grep '^AWS_S3_ACCESS_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
AWS_S3_SECRET_KEY="${AWS_S3_SECRET_KEY:-$(grep '^AWS_S3_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-$(grep '^MINIO_ACCESS_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-$(grep '^MINIO_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2- || true)}"
ALIYUN_OSS_ACCESS_KEY_ID="${ALIYUN_OSS_ACCESS_KEY_ID:-$(grep '^ALIYUN_OSS_ACCESS_KEY_ID=' "$ENV_FILE" | cut -d= -f2- || true)}"
ALIYUN_OSS_ACCESS_KEY_SECRET="${ALIYUN_OSS_ACCESS_KEY_SECRET:-$(grep '^ALIYUN_OSS_ACCESS_KEY_SECRET=' "$ENV_FILE" | cut -d= -f2- || true)}"

# Defaults / 默认值
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-resume_pass}"
RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}"
POSTGRES_USER="${POSTGRES_USER:-resume_user}"
POSTGRES_DB="${POSTGRES_DB:-resume_assistant}"
RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-guest}"

cat <<EOF
---
apiVersion: v1
kind: Secret
metadata:
  name: ${SECRET_NAME}
  namespace: ${NAMESPACE}
type: Opaque
stringData:
  JWT_SECRET: "${JWT_SECRET}"
  INTERNAL_API_KEY: "${INTERNAL_API_KEY}"
  SPRING_DATASOURCE_USERNAME: "${POSTGRES_USER}"
  SPRING_DATASOURCE_PASSWORD: "${POSTGRES_PASSWORD}"
  POSTGRES_USER: "${POSTGRES_USER}"
  POSTGRES_PASSWORD: "${POSTGRES_PASSWORD}"
  POSTGRES_DB: "${POSTGRES_DB}"
  SPRING_RABBITMQ_USERNAME: "${RABBITMQ_USERNAME}"
  SPRING_RABBITMQ_PASSWORD: "${RABBITMQ_PASSWORD}"
  RABBITMQ_USERNAME: "${RABBITMQ_USERNAME}"
  RABBITMQ_PASSWORD: "${RABBITMQ_PASSWORD}"
  REDIS_PASSWORD: "${REDIS_PASSWORD}"
  GEMINI_API_KEY: "${GEMINI_API_KEY}"
  OPENAI_API_KEY: "${OPENAI_API_KEY}"
  ANTHROPIC_API_KEY: "${ANTHROPIC_API_KEY}"
  SMTP_PASSWORD: "${SMTP_PASSWORD}"
  AWS_S3_ACCESS_KEY: "${AWS_S3_ACCESS_KEY}"
  AWS_S3_SECRET_KEY: "${AWS_S3_SECRET_KEY}"
  MINIO_ACCESS_KEY: "${MINIO_ACCESS_KEY}"
  MINIO_SECRET_KEY: "${MINIO_SECRET_KEY}"
  ALIYUN_OSS_ACCESS_KEY_ID: "${ALIYUN_OSS_ACCESS_KEY_ID}"
  ALIYUN_OSS_ACCESS_KEY_SECRET: "${ALIYUN_OSS_ACCESS_KEY_SECRET}"
  VERTEX_PROJECT_ID: "jobcopilot-ai-service"
  VERTEX_LOCATION: "us-central1"
EOF

echo ""
echo "# Secret manifest generated. Apply with:"
echo "#   kubectl apply -f -"
echo "# Secret 清单已生成。使用以下命令应用："
echo "#   kubectl apply -f -"
