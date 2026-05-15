#!/bin/sh
set -e

# Create upload and log directories if they do not exist
# 创建上传和日志目录（如不存在）
mkdir -p /app/uploads /app/backend/logs

# If already running as non-root (e.g. K8s securityContext), execute directly.
# Otherwise (Docker Compose root), switch to resume user via su-exec.
# 如果已经是非 root 运行（如 K8s securityContext），直接执行。
# 否则（Docker Compose 以 root 启动）通过 su-exec 切换到 resume 用户。
if [ "$(id -u)" -eq 0 ]; then
    exec su-exec resume "$@"
else
    exec "$@"
fi
