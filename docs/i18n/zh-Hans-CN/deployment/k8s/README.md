# Kubernetes 部署指南

> [English](../../../../deployment/k8s/README.md) | [简体中文](README.md) | [繁體中文](../../zh-Hant-TW/deployment/k8s/README.md)

## 概述

本目录为 JobCopilot 平台提供**生产级** Kubernetes 部署配置。

### 三种部署方式

| 方式 | 适用场景 | 文件位置 |
|------|----------|----------|
| **Helm Chart** | 生产环境、多环境、版本化发布 | [`helm/jobcopilot/`](../../../../deployment/k8s/helm/jobcopilot/) |
| **ArgoCD GitOps** | 使用 GitOps 工作流的团队、审计追踪 | [`argocd/`](../../../../deployment/k8s/argocd/) |
| **原生 YAML + Kustomize** | 快速测试、学习、无 Helm 的 CI | [`plain-yaml/`](../../../../deployment/k8s/plain-yaml/) |

### 两种基础设施模式

| 模式 | 说明 | 适用时机 |
|------|------|----------|
| **自建 (Embedded)** | PostgreSQL + RabbitMQ + Redis 以 StatefulSet 运行在 K8s 内 | 开发、测试、小型集群 |
| **托管 (Managed)** | 使用云厂商托管数据库 / 消息队列 / 缓存 (RDS, CloudAMQP, ElastiCache) | **生产环境推荐** |

通过 Helm values 切换模式：`global.infraMode`（`embedded` 或 `managed`）。

---

## 前置条件

- **Kubernetes** 1.28+ 集群（托管或自建）
- 已配置集群访问的 **kubectl**
- **Helm** 3.14+（用于 Helm 部署）
- **Ingress Controller**（nginx-ingress、Traefik、AWS ALB 等）
- **cert-manager**（可选，用于自动 TLS）
- **StorageClass**（用于自建模式的数据库 PVC）

---

## 快速开始

### 1. 准备密钥

```bash
# 从 .env 文件生成
./scripts/generate-secrets.sh .env JobCopilot | kubectl apply -f -

# 或手动创建
kubectl create secret generic JobCopilot-secrets \
  --namespace=JobCopilot \
  --from-literal=JWT_SECRET=$(openssl rand -base64 48) \
  --from-literal=POSTGRES_PASSWORD=$(openssl rand -base64 24) \
  --from-literal=INTERNAL_API_KEY=$(openssl rand -base64 32) \
  --from-literal=GEMINI_API_KEY=your-gemini-key
```

### 2. 使用 Helm 部署（推荐）

```bash
# 自建模式（开发）
helm install JobCopilot ./helm/jobcopilot \
  --namespace JobCopilot \
  --create-namespace \
  -f ./helm/jobcopilot/values.yaml \
  -f ./helm/jobcopilot/values-minimal.yaml

# 托管模式（生产）
helm install JobCopilot ./helm/jobcopilot \
  --namespace JobCopilot \
  --create-namespace \
  -f ./helm/jobcopilot/values.yaml \
  -f ./helm/jobcopilot/values-production.yaml
```

### 3. 验证

```bash
kubectl get pods -n JobCopilot
kubectl get ingress -n JobCopilot

# 检查后端健康
kubectl port-forward svc/JobCopilot-backend 8080:8080 -n JobCopilot
curl http://localhost:8080/api/actuator/health

# 检查 AI 服务健康
kubectl port-forward svc/JobCopilot-ai-service 8000:8000 -n JobCopilot
curl http://localhost:8000/health
```

---

## 架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Ingress Controller                              │
│                         (TLS 终止、路径路由)                                  │
└─────────────────────────────┬───────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
        /api/*                             /* (SPA)
              │                               │
    ┌─────────▼─────────┐          ┌─────────▼─────────┐
    │     后端 (Backend) │          │     前端 (Frontend)│
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
│           AI 服务 (AI Service)       │
│        (Python FastAPI)              │
│         replicas: 3                  │
└─────────────────────────────────────┘
```

### 网络隔离

三层 NetworkPolicy 隔离：
- **第一层**: Ingress -> 仅前端
- **第二层**: 后端 <-> AI 服务 <-> RabbitMQ <-> Redis
- **第三层**: 后端 -> 仅 PostgreSQL

---

## 存储建议

### 文件存储（后端 <-> AI 服务共享文件）

Docker Compose 使用共享卷。在 K8s 中，**生产环境不要使用 ReadWriteMany PVC**。

| 选项 | 配置 | 推荐度 |
|--------|-------|----------------|
| **S3 / 兼容 S3** | `STORAGE_TYPE=s3` | **生产环境推荐** |
| **MinIO** | `STORAGE_TYPE=minio` | 私有云 / 内网 |
| **阿里云 OSS** | `STORAGE_TYPE=oss` | 阿里云环境 |
| **ReadWriteMany PVC** | `STORAGE_TYPE=local` | 最后手段；需要 NFS/CephFS CSI |

---

## 目录结构

```
k8s/
├── README.md                          # 本文件
├── helm/
│   └── JobCopilot/
│       ├── Chart.yaml
│       ├── values.yaml                # 默认值（自建、开发）
│       ├── values-production.yaml     # 生产覆盖（托管中间件）
│       ├── values-minimal.yaml        # 最小资源（kind/minikube）
│       └── templates/                 # 所有 K8s 资源
├── argocd/
│   ├── app-of-apps/
│   │   └── JobCopilot-root.yaml
│   └── applications/
│       ├── JobCopilot-dev.yaml
│       ├── JobCopilot-staging.yaml
│       └── JobCopilot-prod.yaml
├── plain-yaml/
│   ├── base/                          # Kustomize 基础资源
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
# 检查 PVC 绑定
kubectl get pvc -n JobCopilot
kubectl describe pvc <name> -n JobCopilot

# 确保 StorageClass 存在
kubectl get storageclass
```

### 后端无法连接 PostgreSQL

```bash
# 验证 NetworkPolicy 允许后端访问 PostgreSQL
kubectl get networkpolicies -n JobCopilot

# 检查 Pod 内 DNS 解析
kubectl exec -it deploy/JobCopilot-backend -n JobCopilot -- nslookup JobCopilot-postgres
```

### Ingress 不工作

```bash
# 验证 Ingress Controller 已安装
kubectl get pods -n ingress-nginx

# 检查 Ingress 事件
kubectl describe ingress JobCopilot -n JobCopilot
```

---

## 安全加固

- 所有容器以 **非 root** 用户运行
- 尽可能启用 **readOnlyRootFilesystem**
- **NetworkPolicies** 强制三层隔离
- 通过 **existingSecret** 或 **External Secrets Operator** 管理密钥
- **PodDisruptionBudgets** 防止自愿中断
- **Resource quotas** 防止 noisy-neighbor 问题

---

## 贡献

添加新环境变量时：
1. 将非敏感变量加入 `templates/configmap.yaml`
2. 将敏感变量加入 `templates/secrets.yaml` 和 `scripts/generate-secrets.sh`
3. 在 `values.yaml` 中设置默认值
4. 在本 README 中补充文档

---

## 许可证

与主项目一致。参见 [../../../LICENSE](../../../../../LICENSE)。
