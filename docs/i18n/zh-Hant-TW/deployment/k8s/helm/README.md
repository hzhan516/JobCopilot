# JobCopilot Helm Chart

> [English](../../../../../deployment/k8s/helm/jobcopilot/README.md) | [簡體中文](../../../../zh-Hans-CN/deployment/k8s/helm/README.md) | [繁體中文](README.md)

## 概述

本 Helm Chart 在 Kubernetes 上部署完整的 JobCopilot 平台。

## 安裝

### 前置條件

```bash
# 如需依賴 chart 可新增 bitnami 倉庫（可選）
# helm repo add bitnami https://charts.bitnami.com/bitnami
# helm repo update
```

### 快速安裝

```bash
# 先建立命名空間
kubectl create namespace JobCopilot

# 使用預設值安裝（自建中介軟體，開發配置）
helm install JobCopilot . \
  --namespace JobCopilot

# 生產環境安裝（託管中介軟體）
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml

# 最小資源安裝（kind/minikube）
helm install JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-minimal.yaml
```

### 升級

```bash
helm upgrade JobCopilot . \
  --namespace JobCopilot \
  -f values.yaml \
  -f values-production.yaml
```

### 解除安裝

```bash
helm uninstall JobCopilot --namespace JobCopilot
# 注意：預設不會刪除 PVC，以防止資料遺失
kubectl delete pvc -n JobCopilot -l app.kubernetes.io/name=JobCopilot
```

---

## 配置

### 關鍵參數

| 參數 | 說明 | 預設值 |
|-----------|-------------|---------|
| `global.infraMode` | 中介軟體模式：`embedded` 或 `managed` | `embedded` |
| `secrets.existingSecret` | 使用預先建立的 Secret 替代 Helm 管理 | `""` |
| `ingress.host` | Ingress 網域 | `JobCopilot.local` |
| `ingress.tls.enabled` | 啟用 TLS | `false` |
| `backend.replicaCount` | 後端副本數 | `1` |
| `backend.springProfilesActive` | Spring 設定檔 | `dev` |
| `backend.storageType` | 檔案儲存：`local`、`s3`、`minio`、`oss` | `local` |
| `aiService.replicaCount` | AI 服務副本數 | `1` |
| `aiService.llmEmbeddingDimension` | 嵌入向量維度 | `1536` |
| `postgres.enabled` | 部署 PostgreSQL StatefulSet | `true` |
| `rabbitmq.enabled` | 部署 RabbitMQ StatefulSet | `true` |
| `redis.enabled` | 部署 Redis StatefulSet | `true` |

### 完整值參考

參見 [`values.yaml`](../../../../../deployment/k8s/helm/jobcopilot/values.yaml)。

---

## 金鑰管理

### 方式一：Helm 管理 Secret（僅開發）

直接在 values 中設定金鑰：
```yaml
secrets:
  jwtSecret: "your-secret"
  postgresPassword: "your-password"
```

### 方式二：預先建立 Secret（生產推薦）

```bash
# 手動建立 Secret
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

設定 ESO 從 AWS Secrets Manager、Azure Key Vault 或 GCP Secret Manager 同步。

---

## PostgreSQL 初始化

當 `global.infraMode=embedded` 時，Chart 會將 `init.sql` 和 `init-db.sh` 掛載到 PostgreSQL Pod 中。初始化指令碼會：

1. 建立所有表（users、resumes、jobs、conversations 等）
2. 啟用 `pgvector` 擴充套件
3. 將 `vector(1536)` 替換為設定的維度
4. 如果維度 <= 2000，建立 HNSW 索引

提供 `init.sql`：
```bash
helm install JobCopilot . \
  --set-file postgres.initSql=../../../backend/app/src/main/resources/db/init.sql
```

生產環境使用 `managed` 模式時，Flyway 會自動處理遷移。

---

## GCP Vertex AI 憑證

掛載 GCP 服務帳號金鑰：

```bash
# 對金鑰檔案進行 Base64 編碼
export GCP_CREDS_B64=$(base64 -w 0 /path/to/gcp-service-account.json)

helm install JobCopilot . \
  --set secrets.gcpCredentialsJson="$GCP_CREDS_B64"
```

Chart 會建立獨立的 Secret，並以唯讀卷形式掛載到 `/app/credentials/gcp-service-account.json`。

---

## 驗證 Chart

```bash
# 語法檢查
helm lint .

# 渲染模板而不安裝
helm template JobCopilot . \
  -f values.yaml \
  -f values-production.yaml > rendered.yaml

# 使用驗證指令碼
../../scripts/validate-manifests.sh
```

---

## 自訂映像檔

```yaml
global:
  imageRegistry: "ghcr.io/your-org/"

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

## 高可用性

生產環境高可用建議：

1. 使用 **託管中介軟體**（`global.infraMode=managed`）-- RDS/Cloud SQL、Amazon MQ/MemoryStore
2. 啟用 **HPA**（`backend.autoscaling.enabled=true`、`aiService.autoscaling.enabled=true`）
3. 啟用 **PDB**（`pdb.enabled=true`）
4. 所有無狀態服務執行 **多副本**
5. 使用 **反親和性** 規則將 Pod 分散到不同節點（如需可加入 values）

本 Chart 不在 K8s 內建立 PostgreSQL/RabbitMQ/Redis 的高可用叢集。
如需自建高可用，可考慮：
- PostgreSQL: [CloudNativePG](https://cloudnative-pg.io/)
- RabbitMQ: [RabbitMQ Cluster Operator](https://www.rabbitmq.com/kubernetes/operator/operator-overview)
- Redis: [Redis Operator](https://github.com/OT-CONTAINER-KIT/redis-operator)
