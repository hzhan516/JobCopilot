# JobCopilot Helm Chart

> [English](../../../../../../deployment/k8s/helm/jobcopilot/README.md) | [简体中文](README.md) | [繁體中文](../../../../../zh-Hant-TW/deployment/k8s/helm/jobcopilot/README.md)

## 概览

此 Helm chart 用于在 Kubernetes 上部署完整的 JobCopilot 平台。

## 安装

### 前置条件

```bash
# 如需依赖 chart，可添加 bitnami 仓库（可选）
# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo update
```

### 快速安装

```bash
# 先创建命名空间
kubectl create namespace jobcopilot

# 使用默认值安装（内置中间件，dev profile）
helm install jobcopilot . \
  --namespace jobcopilot

# 生产安装（托管中间件）
helm install jobcopilot . \
  --namespace jobcopilot \
  -f values.yaml \
  -f values-production.yaml

# 最小资源安装（kind/minikube）
helm install jobcopilot . \
  --namespace jobcopilot \
  -f values.yaml \
  -f values-minimal.yaml
```

### 升级

```bash
helm upgrade jobcopilot . \
  --namespace jobcopilot \
  -f values.yaml \
  -f values-production.yaml
```

### 卸载

```bash
helm uninstall jobcopilot --namespace jobcopilot
# 注意：PVC 默认不会删除，以避免数据丢失
kubectl delete pvc -n jobcopilot -l app.kubernetes.io/name=jobcopilot
```

---

## 配置

### 关键参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `global.infraMode` | 中间件模式：`embedded` 或 `managed` | `embedded` |
| `secrets.existingSecret` | 使用预创建 Secret，而不是 Helm 管理的 Secret | `""` |
| `ingress.host` | Ingress 域名 | `jobcopilot.local` |
| `ingress.tls.enabled` | 启用 TLS | `false` |
| `backend.replicaCount` | 后端副本数 | `1` |
| `backend.springProfilesActive` | Spring profile | `dev` |
| `backend.storageType` | 文件存储：`local`、`s3`、`minio`、`oss` | `local` |
| `aiService.replicaCount` | AI 服务副本数 | `1` |
| `aiService.llmEmbeddingDimension` | 嵌入向量维度 | `1536` |
| `aiWorker.enabled` | 部署后台 AI worker | `true` |
| `aiWorker.replicaCount` | AI worker 副本数 | `1` |
| `postgres.enabled` | 部署 PostgreSQL StatefulSet | `true` |
| `rabbitmq.enabled` | 部署 RabbitMQ StatefulSet | `true` |
| `redis.enabled` | 部署 Redis StatefulSet | `true` |
| `minio.enabled` | 在 embedded 模式部署 MinIO 模型制品存储 | `true` |

### 完整 Values 参考

所有可配置项见 [`values.yaml`](../../../../../../deployment/k8s/helm/jobcopilot/values.yaml)。

---

## Secret 管理

### 方案 1：Helm 管理 Secret（仅开发）

直接在 values 中设置密钥：

```yaml
secrets:
  jwtSecret: "your-secret"
  postgresPassword: "your-password"
```

### 方案 2：预创建 Secret（生产推荐）

```bash
# 手动创建 Secret
kubectl create secret generic jobcopilot-secrets \
  --namespace=jobcopilot \
  --from-literal=JWT_SECRET=... \
  --from-literal=POSTGRES_PASSWORD=... \
  --from-literal=RABBITMQ_PASSWORD=... \
  --from-literal=REDIS_PASSWORD=... \
  --from-literal=MINIO_ACCESS_KEY=... \
  --from-literal=MINIO_SECRET_KEY=... \
  --from-literal=INTERNAL_API_KEY=...

# 在安装时引用
helm install jobcopilot . \
  --set secrets.existingSecret=jobcopilot-secrets
```

### 方案 3：External Secrets Operator

```yaml
secrets:
  existingSecret: "jobcopilot-eso-secret"
```

可配置 ESO 从 AWS Secrets Manager、Azure Key Vault 或 GCP Secret Manager 同步密钥。

---

## PostgreSQL 初始化

当 `global.infraMode=embedded` 时，chart 会把 `init.sql` 和 `init-db.sh` 挂载到 PostgreSQL Pod。初始化脚本会：

1. 创建所有表（users、resumes、jobs、conversations 等）
2. 启用 `pgvector` 扩展
3. 将 `vector(1536)` 替换为配置的维度
4. 在维度 <= 2000 时创建 HNSW 索引

提供 `init.sql`：

```bash
helm install jobcopilot . \
  --set-file postgres.initSql=../../../backend/app/src/main/resources/db/init.sql
```

生产环境使用 `managed` 模式时，Flyway 会自动处理迁移。

---

## GCP Vertex AI 凭据

挂载 GCP 服务账号密钥：

```bash
# Base64 编码密钥文件
export GCP_CREDS_B64=$(base64 -w 0 /path/to/gcp-service-account.json)

helm install jobcopilot . \
  --set secrets.gcpCredentialsJson="$GCP_CREDS_B64"
```

chart 会创建独立 Secret，并以只读卷挂载到 `/app/credentials/gcp-service-account.json`。

---

## 验证 Chart

```bash
# Lint
helm lint .

# 只渲染模板，不安装
helm template jobcopilot . \
  -f values.yaml \
  -f values-production.yaml > rendered.yaml

# 使用验证脚本
../../scripts/validate-manifests.sh
```

---

## 自定义镜像

```yaml
global:
  imageRegistry: "ghcr.io/<owner>/jobcopilot/"

backend:
  image:
    repository: jobcopilot-backend
    tag: "v1.2.3"

aiService:
  image:
    repository: jobcopilot-ai-service
    tag: "v1.2.3"

aiWorker:
  image:
    repository: jobcopilot-ai-service
    tag: "v1.2.3"

frontend:
  image:
    repository: jobcopilot-frontend
    tag: "v1.2.3"
```

---

## 高可用

生产高可用建议：

1. 使用**托管中间件**（`global.infraMode=managed`）：RDS/Cloud SQL、Amazon MQ/MemoryStore
2. 启用 **HPA**（`backend.autoscaling.enabled=true`、`aiService.autoscaling.enabled=true`）
3. 启用 **PDB**（`pdb.enabled=true`）
4. 为所有无状态服务运行多个副本
5. 使用 anti-affinity 规则把 Pod 分散到不同节点（必要时加入 values）

该 chart 不会在 Kubernetes 内构建 PostgreSQL/RabbitMQ/Redis 高可用集群。
如需内置高可用，可考虑：

- PostgreSQL: [CloudNativePG](https://cloudnative-pg.io/)
- RabbitMQ: [RabbitMQ Cluster Operator](https://www.rabbitmq.com/kubernetes/operator/operator-overview)
- Redis: [Redis Operator](https://github.com/OT-CONTAINER-KIT/redis-operator)
