# JobCopilot Helm Chart

> [English](../../../../../deployment/k8s/helm/jobcopilot/README.md) | [简体中文](README.md) | [繁體中文](../../../../zh-Hant-TW/deployment/k8s/helm/README.md)

## 概述

本 Helm Chart 在 Kubernetes 上部署完整的 JobCopilot 平台。

## 安装

### 前置条件

```bash
# 如需依赖 chart 可添加 bitnami 仓库（可选）
# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo update
```

### 快速安装

```bash
# 先创建命名空间
kubectl create namespace JobCopilot

# 使用默认值安装（自建中间件，开发配置）
helm install JobCopilot . \
  --namespace JobCopilot

# 生产环境安装（托管中间件）
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml

# 最小资源安装（kind/minikube）
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-minimal.yaml
```

### 升级

```bash
helm upgrade JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml
```

### 卸载

```bash
helm uninstall JobCopilot --namespace JobCopilot
# 注意：默认不会删除 PVC，以防止数据丢失
kubectl delete pvc -n JobCopilot -l app.kubernetes.io/name=JobCopilot
```

---

## 配置

### 关键参数

| 参数                                | 说明                              | 默认值                      |
|-----------------------------------|---------------------------------|--------------------------|
| `global.infraMode`                | 中间件模式：`embedded` 或 `managed`    | `embedded`               |
| `secrets.existingSecret`          | 使用预创建的 Secret 替代 Helm 管理        | `""`                     |
| `ingress.host`                    | Ingress 域名                      | `JobCopilot.local` |
| `ingress.tls.enabled`             | 启用 TLS                          | `false`                  |
| `backend.replicaCount`            | 后端副本数                           | `1`                      |
| `backend.springProfilesActive`    | Spring 配置文件                     | `dev`                    |
| `backend.storageType`             | 文件存储：`local`、`s3`、`minio`、`oss` | `local`                  |
| `aiService.replicaCount`          | AI 服务副本数                        | `1`                      |
| `aiService.llmEmbeddingDimension` | 嵌入向量维度                          | `1536`                   |
| `postgres.enabled`                | 部署 PostgreSQL StatefulSet       | `true`                   |
| `rabbitmq.enabled`                | 部署 RabbitMQ StatefulSet         | `true`                   |
| `redis.enabled`                   | 部署 Redis StatefulSet            | `true`                   |

### 完整值参考

参见 [`values.yaml`](../../../../../deployment/k8s/helm/jobcopilot/values.yaml)。

---

## 密钥管理

### 方式一：Helm 管理 Secret（仅开发）

直接在 values 中设置密钥：
```yaml
secrets:
  jwtSecret: "your-secret"
  postgresPassword: "your-password"
```

### 方式二：预创建 Secret（生产推荐）

```bash
# 手动创建 Secret
kubectl create secret generic JobCopilot-secrets \
  --namespace=JobCopilot \
  --from-literal=JWT_SECRET=... \
  --from-literal=POSTGRES_PASSWORD=...

# 在 values 中引用
helm install JobCopilot . \
  --set secrets.existingSecret=JobCopilot-secrets
```

### 方式三：External Secrets Operator

```yaml
secrets:
  existingSecret: "JobCopilot-eso-secret"
```

配置 ESO 从 AWS Secrets Manager、Azure Key Vault 或 GCP Secret Manager 同步。

---

## PostgreSQL 初始化

当 `global.infraMode=embedded` 时，Chart 会将 `init.sql` 和 `init-db.sh` 挂载到 PostgreSQL Pod 中。初始化脚本会：

1. 创建所有表（users、resumes、jobs、conversations 等）
2. 启用 `pgvector` 扩展
3. 将 `vector(1536)` 替换为配置的维度
4. 如果维度 <= 2000，创建 HNSW 索引

提供 `init.sql`：
```bash
helm install JobCopilot . \
  --set-file postgres.initSql=../../../backend/app/src/main/resources/db/init.sql
```

生产环境使用 `managed` 模式时，Flyway 会自动处理迁移。

---

## GCP Vertex AI 凭证

挂载 GCP 服务账号密钥：

```bash
# 对密钥文件进行 Base64 编码
export GCP_CREDS_B64=$(base64 -w 0 /path/to/gcp-service-account.json)

helm install JobCopilot . \
  --set secrets.gcpCredentialsJson="$GCP_CREDS_B64"
```

Chart 会创建独立的 Secret，并以只读卷形式挂载到 `/app/credentials/gcp-service-account.json`。

---

## 验证 Chart

```bash
# 语法检查
helm lint .

# 渲染模板而不安装
helm template JobCopilot . \
  -f values.yaml \
  -f values-production.yaml > rendered.yaml

# 使用验证脚本
../../scripts/validate-manifests.sh
```

---

## 自定义镜像

```yaml
global:
  imageRegistry: "ghcr.io/jobcopilot/"

backend:
  image:
    repository: JobCopilot-backend
    tag: "v1.2.3"

aiService:
  image:
    repository: JobCopilot-ai-service
    tag: "v1.2.3"

frontend:
  image:
    repository: JobCopilot-frontend
    tag: "v1.2.3"
```

---

## 高可用

生产环境高可用建议：

1. 使用 **托管中间件**（`global.infraMode=managed`）-- RDS/Cloud SQL、Amazon MQ/MemoryStore
2. 启用 **HPA**（`backend.autoscaling.enabled=true`、`aiService.autoscaling.enabled=true`）
3. 启用 **PDB**（`pdb.enabled=true`）
4. 所有无状态服务运行 **多副本**
5. 使用 **反亲和性** 规则将 Pod 分散到不同节点（如需可加入 values）

本 Chart 不在 K8s 内构建 PostgreSQL/RabbitMQ/Redis 的高可用集群。
如需自建高可用，可考虑：
- PostgreSQL: [CloudNativePG](https://cloudnative-pg.io/)
- RabbitMQ: [RabbitMQ Cluster Operator](https://www.rabbitmq.com/kubernetes/operator/operator-overview)
- Redis: [Redis Operator](https://github.com/OT-CONTAINER-KIT/redis-operator)
