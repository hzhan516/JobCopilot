# 原生 YAML + Kustomize 部署

> [English](../../../../../deployment/k8s/plain-yaml/README.md) | [簡體中文](../../../../zh-Hans-CN/deployment/k8s/plain-yaml/README.md) | [繁體中文](README.md)

## 概述

本目錄為不使用 Helm 的環境提供原始 Kubernetes YAML 清單及 Kustomize overlay。

## 目錄結構

```
plain-yaml/
├── kustomization.yaml          # 基礎 Kustomization
├── base/                       # 基礎資源
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secrets-placeholder.yaml
│   ├── network-policies.yaml
│   ├── ingress.yaml
│   ├── postgres/
│   ├── rabbitmq/
│   ├── redis/
│   ├── minio/
│   ├── backend/
│   ├── ai-service/
│   ├── ai-worker/
│   └── frontend/
└── overlays/
    ├── development/
    └── production/
```

## 快速開始

### 1. 建立金鑰

```bash
# 從 .env 生成
../scripts/generate-secrets.sh .env jobcopilot | kubectl apply -f -

# 或手動建立
kubectl create secret generic jobcopilot-secrets \
  --namespace=jobcopilot \
  --from-literal=JWT_SECRET=your-secret \
  --from-literal=POSTGRES_PASSWORD=your-password \
  --from-literal=RABBITMQ_PASSWORD=your-rabbitmq-password \
  --from-literal=REDIS_PASSWORD=your-redis-password \
  --from-literal=MINIO_ACCESS_KEY=your-minio-access-key \
  --from-literal=MINIO_SECRET_KEY=your-minio-secret-key \
  --from-literal=INTERNAL_API_KEY=your-internal-api-key
```

### 2. 部署基礎層

```bash
# 應用所有基礎資源
kubectl apply -k .
```

### 3. 使用 Overlay 部署

```bash
# 開發環境
kubectl apply -k overlays/development/

# 生產環境
kubectl apply -k overlays/production/
```

## 自訂

### 修改基礎資源

直接編輯 `base/` 下的檔案。所有 overlay 繼承自基礎層。

### 建立新 Overlay

```bash
mkdir overlays/my-env
cat > overlays/my-env/kustomization.yaml <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: jobcopilot-my-env

resources:
  - ../../base

namePrefix: my-env-

commonLabels:
  environment: my-env

patches:
  - target:
      kind: Ingress
      name: jobcopilot
    patch: |
      - op: replace
        path: /spec/rules/0/host
        value: my-env.jobcopilot.example.com
EOF
```

### 新增資源限制

在 overlay 中新增補丁：

```yaml
patches:
  - target:
      kind: Deployment
      name: jobcopilot-backend
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

### 排除自建中介軟體

使用託管中介軟體模式時，從 kustomization 中移除 StatefulSet 資源：

```yaml
resources:
  - base/namespace.yaml
  - base/configmap.yaml
  # ... 其他資源 ...
  # 不要包含 postgres/、rabbitmq/、redis/、minio/ 的 StatefulSet
```

## 不使用 Kustomize

你也可以直接逐個應用檔案：

```bash
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
# ... 按順序應用每個檔案 ...
```

推薦順序：
1. Namespace、ConfigMap、Secret
2. NetworkPolicies
3. PostgreSQL、RabbitMQ、Redis、MinIO（StatefulSets + Services）
4. Backend、AI Service、AI Worker、Frontend（Deployments + Services）
5. Ingress

## 驗證

```bash
# 試執行
kubectl apply -k . --dry-run=client

# 伺服端驗證
kubectl apply -k . --dry-run=server
```
