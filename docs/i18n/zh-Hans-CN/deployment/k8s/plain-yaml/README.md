# 原生 YAML + Kustomize 部署

> [English](../../../../../../deployment/k8s/plain-yaml/README.md) | [简体中文](README.md) | [繁體中文](../../../../../zh-Hant-TW/deployment/k8s/plain-yaml/README.md)

## 概述

本目录为不使用 Helm 的环境提供原始 Kubernetes YAML 清单及 Kustomize overlay。

## 目录结构

```
plain-yaml/
├── kustomization.yaml          # 基础 Kustomization
├── base/                       # 基础资源
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets-placeholder.yaml
│   ├── network-policies.yaml
│   ├── ingress.yaml
│   ├── postgres/
│   ├── rabbitmq/
│   ├── redis/
│   ├── backend/
│   ├── ai-service/
│   └── frontend/
└── overlays/
    ├── development/
    └── production/
```

## 快速开始

### 1. 创建密钥

```bash
# 从 .env 生成
../scripts/generate-secrets.sh .env JobCopilot | kubectl apply -f -

# 或手动创建
kubectl create secret generic JobCopilot-secrets \
  --namespace=JobCopilot \
  --from-literal=JWT_SECRET=your-secret \
  --from-literal=POSTGRES_PASSWORD=your-password
```

### 2. 部署基础层

```bash
# 应用所有基础资源
kubectl apply -k .
```

### 3. 使用 Overlay 部署

```bash
# 开发环境
kubectl apply -k overlays/development/

# 生产环境
kubectl apply -k overlays/production/
```

## 自定义

### 修改基础资源

直接编辑 `base/` 下的文件。所有 overlay 继承自基础层。

### 创建新 Overlay

```bash
mkdir overlays/my-env
cat > overlays/my-env/kustomization.yaml <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: JobCopilot-my-env

resources:
  - ../../base

namePrefix: my-env-

commonLabels:
  environment: my-env

patches:
  - target:
      kind: Ingress
      name: JobCopilot
    patch: |
      - op: replace
        path: /spec/rules/0/host
        value: my-env.JobCopilot.example.com
EOF
```

### 添加资源限制

在 overlay 中添加补丁：

```yaml
patches:
  - target:
      kind: Deployment
      name: JobCopilot-backend
    patch: |
      - op: add
        path: /spec/template/spec/containers/0/resources
        value:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
```

### 排除自建中间件

使用托管中间件模式时，从 kustomization 中移除 StatefulSet 资源：

```yaml
resources:
  - base/namespace.yaml
  - base/configmap.yaml
  # ... 其他资源 ...
  # 不要包含 postgres/、rabbitmq/、redis/ 的 StatefulSet
```

## 不使用 Kustomize

你也可以直接逐个应用文件：

```bash
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
# ... 按顺序应用每个文件 ...
```

推荐顺序：
1. Namespace、ConfigMap、Secret
2. NetworkPolicies
3. PostgreSQL、RabbitMQ、Redis（StatefulSets + Services）
4. Backend、AI Service、Frontend（Deployments + Services）
5. Ingress

## 验证

```bash
# 试运行
kubectl apply -k . --dry-run=client

# 服务端验证
kubectl apply -k . --dry-run=server
```
