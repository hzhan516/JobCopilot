#!/bin/sh
set -e

# Create and fix permissions for upload and log directories
# 创建并修复上传和日志目录权限
mkdir -p /app/uploads /app/logs
chown -R resume:resume /app/uploads /app/logs

# Switch to resume user and run the application
# 切换到 resume 用户并运行应用
exec su-exec resume "$@"
