# Kubernetes 部署指南

> [English](../../../../deployment/k8s/README.md) | [簡體中文](../../zh-Hans-CN/deployment/k8s/README.md) | [繁體中文](README.md)

## 概述

本目錄為 Resume Assistant 平台提供**生產級** Kubernetes 部署配置。

### 三種部署方式

| 方式 | 適用場景 | 檔案位置 |
|------|----------|----------|
| **Helm Chart** | 生產環境、多環境、版本化發佈 | [`helm/resume-assistant/`](../../../../deployment/k8s/helm/resume-assistant/) |
| **ArgoCD GitOps** | 使用 GitOps 工作流的團隊、審計追蹤 | [`argocd/`](../../../../deployment/k8s/argocd/) |
| **原生 YAML + Kustomize** | 快速測試、學習、無 Helm 的 CI | [`plain-yaml/`](../../../../deployment/k8s/plain-yaml/) |

### 兩種基礎設施模式

| 模式 | 說明 | 適用時機 |
|------|------|----------|
| **自建 (Embedded)** | PostgreSQL + RabbitMQ + Redis 以 StatefulSet 執行在 K8s 內 | 開發、測試、小型叢集 |
| **託管 (Managed)** | 使用雲廠商託管資料庫 / 訊息佇列 / 快取 (RDS, CloudAMQP, ElastiCache) | **生產環境推薦** |

透過 Helm values 切換模式：`global.infraMode`（`embedded` 或 `managed`）。

---

## 前置條件

- **Kubernetes** 1.28+ 叢集（託管或自建）
- 已配置叢集存取的 **kubectl**
- **Helm** 3.14+（用於 Helm 部署）
- **Ingress Controller**（nginx-ingress、Traefik、AWS ALB 等）
- **cert-manager**（可選，用於自動 TLS）
- **StorageClass**（用於自建模式的資料庫 PVC）

---

## 快速開始

### 1. 準備金鑰

```bash
# 從 .env 檔案生成
./scripts/generate-secrets.sh .env resume-assistant | kubectl apply -f -

# 或手動建立
kubectl create secret generic resume-assistant-secrets \
  --namespace=resume-assistant \
  --from-literal=JWT_SECRET=$(openssl rand -base64 48) \
  --from-literal=POSTGRES_PASSWORD=$(openssl rand -base64 24) \
  --from-literal=INTERNAL_API_KEY=$(openssl rand -base64 32) \
  --from-literal=GEMINI_API_KEY=your-gemini-key
```

### 2. 使用 Helm 部署（推薦）

```bash
# 自建模式（開發）
helm install resume-assistant ./helm/resume-assistant \
  --namespace resume-assistant \
  --create-namespace \
  -f ./helm/resume-assistant/values.yaml \
  -f ./helm/resume-assistant/values-minimal.yaml

# 託管模式（生產）
helm install resume-assistant ./helm/resume-assistant \
  --namespace resume-assistant \
  --create-namespace \
  -f ./helm/resume-assistant/values.yaml \
  -f ./helm/resume-assistant/values-production.yaml
```

### 3. 驗證

```bash
kubectl get pods -n resume-assistant
kubectl get ingress -n resume-assistant

# 檢查後端健康
kubectl port-forward svc/resume-assistant-backend 8080:8080 -n resume-assistant
curl http://localhost:8080/api/actuator/health

# 檢查 AI 服務健康
kubectl port-forward svc/resume-assistant-ai-service 8000:8000 -n resume-assistant
curl http://localhost:8000/health
```

---

## 架構

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Ingress Controller                              │
│                         (TLS 終止、路徑路由)                                  │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
        /api/*                             /* (SPA)
              │                               │
    ┌─────────▼─────────┐          ┌─────────▼─────────┐
    │     後端 (Backend) │          │     前端 (Frontend)│
    │  (Spring Boot)    │          │  (React + Nginx)  │
    │   replicas: 3     │          │   replicas: 3     │
    └─────────┬─────────┘          └───────────────────┘
              │
    ┌─────────┼─────────┐
    │         │         │
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌─────────────┐
│PostgreSQL│ │RabbitMQ│ │    Redis     │
│(Stateful)│ │(Stateful)│ │  (Stateful)  │
└───────┘ └───────┘ └─────────────┘
    │         │         │
    │    ┌────┘         │
    │    │              │
    ▼    ▼              ▼
┌─────────────────────────────────────┐
│           AI 服務 (AI Service)       │
│        (Python FastAPI)              │
│         replicas: 3                  │
└─────────────────────────────────────┘
```

### 網路隔離

三層 NetworkPolicy 隔離：
- **第一層**: Ingress -> 僅前端
- **第二層**: 後端 <-> AI 服務 <-> RabbitMQ <-> Redis
- **第三層**: 後端 -> 僅 PostgreSQL

---

## 儲存建議

### 檔案儲存（後端 <-> AI 服務共享檔案）

Docker Compose 使用共享卷。在 K8s 中，**生產環境不要使用 ReadWriteMany PVC**。

| 選項 | 配置 | 推薦度 |
|--------|-------|----------------|
| **S3 / 相容 S3** | `STORAGE_TYPE=s3` | **生產環境推薦** |
| **MinIO** | `STORAGE_TYPE=minio` | 私有雲 / 內網 |
| **阿里雲 OSS** | `STORAGE_TYPE=oss` | 阿里雲環境 |
| **ReadWriteMany PVC** | `STORAGE_TYPE=local` | 最後手段；需要 NFS/CephFS CSI |

---

## 目錄結構

```
k8s/
├── README.md                          # 本檔案
├── helm/
│   └── resume-assistant/
│       ├── Chart.yaml
│       ├── values.yaml                # 預設值（自建、開發）
│       ├── values-production.yaml     # 生產覆蓋（託管中介軟體）
│       ├── values-minimal.yaml        # 最小資源（kind/minikube）
│       └── templates/                 # 所有 K8s 資源
├── argocd/
│   ├── app-of-apps/
│   │   └── resume-assistant-root.yaml
│   └── applications/
│       ├── resume-assistant-dev.yaml
│       ├── resume-assistant-staging.yaml
│       └── resume-assistant-prod.yaml
├── plain-yaml/
│   ├── base/                          # Kustomize 基礎資源
│   └── overlays/
│       ├── development/
│       └── production/
└── scripts/
    ├── generate-secrets.sh
    └── validate-manifests.sh
```

---

## 故障排除

### Pod 卡在 Pending

```bash
# 檢查 PVC 綁定
kubectl get pvc -n resume-assistant
kubectl describe pvc <name> -n resume-assistant

# 確保 StorageClass 存在
kubectl get storageclass
```

### 後端無法連線 PostgreSQL

```bash
# 驗證 NetworkPolicy 允許後端存取 PostgreSQL
kubectl get networkpolicies -n resume-assistant

# 檢查 Pod 內 DNS 解析
kubectl exec -it deploy/resume-assistant-backend -n resume-assistant -- nslookup resume-assistant-postgres
```

### Ingress 不工作

```bash
# 驗證 Ingress Controller 已安裝
kubectl get pods -n ingress-nginx

# 檢查 Ingress 事件
kubectl describe ingress resume-assistant -n resume-assistant
```

---

## 安全加固

- 所有容器以 **非 root** 使用者執行
- 盡可能啟用 **readOnlyRootFilesystem**
- **NetworkPolicies** 強制三層隔離
- 透過 **existingSecret** 或 **External Secrets Operator** 管理金鑰
- **PodDisruptionBudgets** 防止自願中斷
- **Resource quotas** 防止 noisy-neighbor 問題

---

## 貢獻

新增環境變數時：
1. 將非敏感變數加入 `templates/configmap.yaml`
2. 將敏感變數加入 `templates/secrets.yaml` 和 `scripts/generate-secrets.sh`
3. 在 `values.yaml` 中設定預設值
4. 在本 README 中補充文件

---

## 授權條款

與主專案一致。參見 [../../../LICENSE](../../../../../LICENSE)。
