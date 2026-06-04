#!/bin/bash
# =============================================================================
# JobCopilot - Prepare minikube host directories for rootless middleware
# 为 rootless minikube 中间件准备宿主机存储目录
# =============================================================================
set -e

echo "Preparing minikube host directories for Bitnami middleware..."

# 检查 minikube 是否运行
if ! minikube status &>/dev/null; then
    echo "ERROR: minikube is not running. Please start minikube first."
    echo "错误：minikube 未运行。请先启动 minikube。"
    exit 1
fi

# 创建并设置父目录权限为 777
# 父目录 777 使 initContainer 能够创建子目录并设置正确的 owner/权限
minikube ssh -- "
    sudo mkdir -p /mnt/data/jobcopilot
    sudo chmod 777 /mnt/data/jobcopilot
    echo 'Host directory ready: /mnt/data/jobcopilot (777)'
    ls -ld /mnt/data/jobcopilot
"

echo ""
echo "Done. You can now deploy with:"
echo "  helm upgrade jobcopilot ./helm/jobcopilot \\"
echo "    --namespace jobcopilot \\"
echo "    -f values.yaml -f values-minimal.yaml"
