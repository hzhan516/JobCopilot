# ArgoCD GitOps 部署

> [English](../../../../../../deployment/k8s/argocd/README.md) | [简体中文](README.md) | [繁體中文](../../../../../zh-Hant-TW/deployment/k8s/argocd/README.md)

## 概述

本目录提供 ArgoCD `Application` 清单，用于通过 GitOps 管理 JobCopilot 平台。

## 架构

```
┌─────────────────────────────────────────┐
│  ArgoCD 根应用 (Root Application)        │
│  (app-of-apps/JobCopilot-root)   │
└─────────────┬───────────────────────────┘
              │ 管理
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐
│  dev  │ │staging│ │  prod │
└───────┘ └───────┘ └───────┘
```

## 前置条件

- 集群上已安装 ArgoCD 2.10+
- 包含本项目的 Git 仓库
- 对 ArgoCD 命名空间（默认：`argocd`）的 kubectl 访问权限

## 设置

### 1. 更新仓库地址

编辑 `applications/` 和 `app-of-apps/` 下的所有文件，指向你的实际 Git 仓库：

```yaml
source:
  repoURL: https://github.com/your-org/JobCopilot.git
  targetRevision: HEAD  # 或分支名
```

### 2. 部署根应用

```bash
kubectl apply -f app-of-apps/JobCopilot-root.yaml
```

这会创建根 Application，自动管理 dev、staging 和 prod 三个环境的 Applications。

### 3. 访问 ArgoCD UI

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# 在浏览器中打开 https://localhost:8080
```

## 环境配置

| 环境 | 命名空间 | 同步策略 | 自动同步 | 副本数 |
|-------------|-----------|-------------|-----------|----------|
| **dev** | `JobCopilot-dev` | 自动 | 是 | 1（最小） |
| **staging** | `JobCopilot-staging` | 自动 | 是 | 2（类生产） |
| **prod** | `JobCopilot-prod` | 手动 | 否 | 3（完整生产） |

### 开发环境 (Dev)

- 使用 `values-minimal.yaml`
- 自动同步和清理
- 单节点资源
- 自建中间件

### 预发布环境 (Staging)

- 使用 `values-production.yaml`
- 自动同步
- 多副本但降低资源
- 可使用托管或自建中间件

### 生产环境 (Prod)

- 使用 `values-production.yaml`
- **手动同步** -- 需要在 ArgoCD UI 中由运维人员批准
- 完整副本数和资源限制
- 推荐使用托管中间件
- 配置 `ignoreDifferences` 处理 HPA 导致的副本数漂移

## 添加新环境

1. 复制 `applications/JobCopilot-dev.yaml`
2. 重命名为 `JobCopilot-<env>.yaml`
3. 更新以下字段：
   - `metadata.name`
   - `destination.namespace`
   - `source.helm.valueFiles`
   - `source.helm.parameters`（host、replicas 等）
   - `syncPolicy`（按需）
4. 将新文件加入 Git
5. ArgoCD 根应用会自动同步

## ArgoCD 密钥管理

切勿在 Git 中存放明文密钥。可选方案：

1. **在目标命名空间预创建 Secrets**
   ```bash
   kubectl create secret generic JobCopilot-secrets \
     --namespace=JobCopilot-prod \
     --from-literal=JWT_SECRET=...
   ```
   然后在 values 中设置 `secrets.existingSecret`。

2. **External Secrets Operator** + ArgoCD
   - 在集群中安装 ESO
   - 创建 `ExternalSecret` 资源（可存入 Git）
   - ESO 从云厂商密钥库同步

3. **Sealed Secrets** (Bitnami)
   - 使用 `kubeseal` 加密密钥
   - 将加密的 `SealedSecret` 存入 Git
   - Controller 自动解密为普通 Secret

## 同步策略

### 自动同步

```yaml
syncPolicy:
  automated:
    prune: true      # 删除 Git 中不存在的资源
    selfHeal: true   # 回滚手动修改
```

### 手动同步

```yaml
syncPolicy:
  automated:
    prune: false
    selfHeal: false
```

使用 ArgoCD UI 或 CLI 执行同步：
```bash
argocd app sync JobCopilot-prod
```

## 监控

查看同步状态：
```bash
argocd app list
argocd app get JobCopilot-prod
```

检查漂移：
```bash
argocd app diff JobCopilot-prod
```
