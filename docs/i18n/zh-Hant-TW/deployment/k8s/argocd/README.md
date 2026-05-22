# ArgoCD GitOps 部署

> [English](../../../../../../deployment/k8s/argocd/README.md) | [簡體中文](../../../../../zh-Hans-CN/deployment/k8s/argocd/README.md) | [繁體中文](README.md)

## 概述

本目錄提供 ArgoCD `Application` 清單，用於透過 GitOps 管理 Resume Assistant 平台。

## 架構

```
┌─────────────────────────────────────────┐
│  ArgoCD 根應用 (Root Application)        │
│  (app-of-apps/resume-assistant-root)   │
└─────────────┬───────────────────────────┘
              │ 管理
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐
│  dev  │ │staging│ │  prod │
└───────┘ └───────┘ └───────┘
```

## 前置條件

- 叢集上已安裝 ArgoCD 2.10+
- 包含本專案的 Git 倉庫
- 對 ArgoCD 命名空間（預設：`argocd`）的 kubectl 存取權限

## 設定

### 1. 更新倉庫位址

編輯 `applications/` 和 `app-of-apps/` 下的所有檔案，指向你的實際 Git 倉庫：

```yaml
source:
  repoURL: https://github.com/your-org/resume-assistant.git
  targetRevision: HEAD  # 或分支名
```

### 2. 部署根應用

```bash
kubectl apply -f app-of-apps/resume-assistant-root.yaml
```

這會建立根 Application，自動管理 dev、staging 和 prod 三個環境的 Applications。

### 3. 存取 ArgoCD UI

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
# 在瀏覽器中開啟 https://localhost:8080
```

## 環境配置

| 環境 | 命名空間 | 同步策略 | 自動同步 | 副本數 |
|-------------|-----------|-------------|-----------|----------|
| **dev** | `resume-assistant-dev` | 自動 | 是 | 1（最小） |
| **staging** | `resume-assistant-staging` | 自動 | 是 | 2（類生產） |
| **prod** | `resume-assistant-prod` | 手動 | 否 | 3（完整生產） |

### 開發環境 (Dev)

- 使用 `values-minimal.yaml`
- 自動同步和清理
- 單節點資源
- 自建中介軟體

### 預發布環境 (Staging)

- 使用 `values-production.yaml`
- 自動同步
- 多副本但降低資源
- 可使用託管或自建中介軟體

### 生產環境 (Prod)

- 使用 `values-production.yaml`
- **手動同步** -- 需要在 ArgoCD UI 中由維運人員批准
- 完整副本數和資源限制
- 推薦使用託管中介軟體
- 設定 `ignoreDifferences` 處理 HPA 導致的副本數漂移

## 新增新環境

1. 複製 `applications/resume-assistant-dev.yaml`
2. 重新命名為 `resume-assistant-<env>.yaml`
3. 更新以下欄位：
   - `metadata.name`
   - `destination.namespace`
   - `source.helm.valueFiles`
   - `source.helm.parameters`（host、replicas 等）
   - `syncPolicy`（按需）
4. 將新檔案加入 Git
5. ArgoCD 根應用會自動同步

## ArgoCD 金鑰管理

切勿在 Git 中存放明文金鑰。可選方案：

1. **在目標命名空間預先建立 Secrets**
   ```bash
   kubectl create secret generic resume-assistant-secrets \
     --namespace=resume-assistant-prod \
     --from-literal=JWT_SECRET=...
   ```
   然後在 values 中設定 `secrets.existingSecret`。

2. **External Secrets Operator** + ArgoCD
   - 在叢集中安裝 ESO
   - 建立 `ExternalSecret` 資源（可存入 Git）
   - ESO 從雲廠商金鑰庫同步

3. **Sealed Secrets** (Bitnami)
   - 使用 `kubeseal` 加密金鑰
   - 將加密的 `SealedSecret` 存入 Git
   - Controller 自動解密為普通 Secret

## 同步策略

### 自動同步

```yaml
syncPolicy:
  automated:
    prune: true      # 刪除 Git 中不存在的資源
    selfHeal: true   # 回滾手動修改
```

### 手動同步

```yaml
syncPolicy:
  automated:
    prune: false
    selfHeal: false
```

使用 ArgoCD UI 或 CLI 執行同步：
```bash
argocd app sync resume-assistant-prod
```

## 監控

檢視同步狀態：
```bash
argocd app list
argocd app get resume-assistant-prod
```

檢查漂移：
```bash
argocd app diff resume-assistant-prod
```
