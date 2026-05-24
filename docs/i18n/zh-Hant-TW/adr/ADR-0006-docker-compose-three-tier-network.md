# ADR-0006: Docker Compose 三層網路架構

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 後端架構團隊 |
| **Affected Files** | `docker-compose.yml`, `.env.example`, `docs/deployment/architecture.md`, `frontend/Dockerfile` |

---

## 1. Context / 背景

ResumeAssistant 面向三種部署角色：

| 角色 | 部署模式 | 網路需求 |
|------|----------|----------|
| **非技術使用者** | 本地可安裝包（VirtualBox / VMware 虛擬機） | 最小攻擊面；一鍵啟動 |
| **技術專業人員** | 本地 Docker Compose 堆疊 | 無需 K8s 複雜度的縱深防禦 |
| **企業使用者** | Kubernetes / 雲原生分發 | 網路策略、服務網格相容 |

三種角色共用一個共同要求：**預設情況下，應用不得將內部服務（資料庫、訊息佇列、AI 推理）暴露給主機或網際網路。**

### 1.1 採用 ADR 前的反模式

早期原型將主機埠直接對應到每個服務：

```yaml
# ❌ 反模式：每個服務都暴露到主機
services:
  backend:
    ports: ["8080:8080"]   # 主機可直接存取 Spring Boot
  postgres:
    ports: ["5432:5432"]   # 主機可直接存取 PostgreSQL
  rabbitmq:
    ports: ["5672:5672"]   # 主機可直接存取 AMQP
```

這違反了最小權限原則：
- 被入侵的主機程序可以直接連接資料庫。
- 開發者會意外地將前端 `VITE_API_BASE_URL` 指向 `http://localhost:8080`，繞過反向代理，使後端暴露於 CORS 和直接攻擊。
- 該架構無法作為本地可安裝包安全發布，因為使用者的筆記型電腦實際上變成了公網可達的伺服器。

### 1.2 候選架構

| 方案 | 說明 | 評估 |
|------|------|------|
| **A. 單一 Docker 網路（bridge）** | 所有容器共用一個網路；服務名稱全域解析 | 簡單，但無橫向移動保護 |
| **B. 三層 Docker 網路（bridge）** | `public`、`internal`、`db` — 顯式分段，後端作為閘道 | 縱深防禦，易於理解，可對應到 K8s NetworkPolicies |
| **C. Docker Compose + host 網路模式** | 容器共用主機網路命名空間 | 最快，但破壞所有隔離；立即拒絕 |
| **D. 本地部署完整 K8s** | Minikube / k3s + NetworkPolicies | 對非技術使用者過重；學習曲線陡峭 |

---

## 2. Decision / 決策

**採用方案 B：三層 Docker bridge 網路架構，顯式定義服務放置位置，後端作為唯一的跨網路閘道。**

### 2.1 分層定義

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                  網際網路（INTERNET）                        │
│                                    │                                        │
│                                    ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────┐      │
│   │  主機（開發者筆記型電腦 / 虛擬機 / 雲實例）                        │      │
│   │                                                                 │      │
│   │   連接埠 80（或 FRONTEND_HOST_PORT）──► ┌─────────────┐        │      │
│   │                                       │   Nginx     │          │      │
│   │                                       │  (frontend) │          │      │
│   │                                       └──────┬──────┘          │      │
│   │                                              │                │      │
│   │   ┌──────────────────────────────────────────┘                │      │
│   │   │         public-network（bridge, /16）                       │      │
│   │   │                                                           │      │
│   │   │    ┌──────────────┐          ┌──────────────┐             │      │
│   │   └───►│   backend    │◄─────────►│   backend    │◄────────────┘      │
│   │        │  :8080       │          │  :8080       │                    │
│   │        │  (Spring Boot)│         │  (Spring Boot)│                    │
│   │        │              │          │              │                    │
│   │        │  同時接入：   │          │  同時接入：   │                    │
│   │        │  internal    │          │  db          │                    │
│   │        └──────┬───────┘          └──────┬───────┘                    │
│   │               │                         │                            │
│   │   ┌───────────┴─────────────────────────┘                            │
│   │   │         internal-network（bridge, /16）                          │
│   │   │                                                                │
│   │   │   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐ │
│   │   │   │ai-api  │  │rabbitmq│  │ redis  │  │ai-worker│  │ minio  │ │
│   │   │   │:8000   │  │:5672   │  │:6379   │  │ (訓練)  │  │:9000   │ │
│   │   │   └────────┘  └────────┘  └────────┘  └────────┘  └────────┘ │
│   │   └────────────────────────────────────────────────────────────────┘
│   │
│   │   ┌────────────────────────────────────────────────────────────────┐
│   │   │         db-network（bridge, /16）                                │
│   │   │                                                              │
│   │   │                    ┌──────────────┐                            │
│   │   │                    │  postgres    │                            │
│   │   │                    │  :5432       │                            │
│   │   │                    │ + pgvector   │                            │
│   │   │                    └──────────────┘                            │
│   │   └────────────────────────────────────────────────────────────────┘
│   └─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 服務與網路對應

| 服務 | public-network | internal-network | db-network | 主機連接埠 | 角色 |
|------|---------------|------------------|------------|------------|------|
| **frontend** (Nginx) | ✅ | ❌ | ❌ | `80:8080` | 唯一 HTTP 入口；反向代理 `/api/*` 到後端 |
| **backend** (Spring Boot) | ✅ | ✅ | ✅ | 無 | 閘道；跨三層 |
| **ai-api** (FastAPI) | ❌ | ✅ | ❌ | 無 | LLM 推理、向量化、解析 |
| **ai-worker** (LightGBM) | ❌ | ✅ | ❌ | 無 | 增量模型訓練 |
| **rabbitmq** | ❌ | ✅ | ❌ | 無 | 非同步訊息代理（Outbox 模式） |
| **redis** | ❌ | ✅ | ❌ | 無 | 快取、分散式鎖（ShedLock）、回饋緩衝 |
| **minio** | ❌ | ✅ | ❌ | 無 | 模型製品倉庫 |
| **postgres** | ❌ | ❌ | ✅ | 無 | 業務資料 + 向量嵌入（pgvector） |

### 2.3 閘道原則

**後端**是唯一接入全部三個網路的容器。這是有意為之：

1. **流量控制**：所有外部 HTTP 請求通過 `frontend:80` → `backend:8080` 進入。後端決定是查詢 PostgreSQL、發布 RabbitMQ 訊息，還是呼叫 AI 服務。
2. **金鑰集中化**：只有後端需要 PostgreSQL 憑證、RabbitMQ 憑證和用於 AI 服務認證的 `INTERNAL_API_KEY`。其他層永遠不會看到跨層金鑰。
3. **可觀測性**：單個請求可以追蹤 `Nginx → backend → (db | mq | ai-api)`，無需跨越網路邊界跳轉。

### 2.4 Docker Compose 實現

```yaml
# docker-compose.yml — 網路配置節選
networks:
  public-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

  internal-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

  db-network:
    driver: bridge
    driver_opts:
      com.docker.network.driver.mtu: 1500

services:
  frontend:
    networks:
      - public-network
    ports:
      - "${FRONTEND_HOST_PORT:-80}:8080"

  backend:
    networks:
      - public-network
      - internal-network
      - db-network
    # 無主機連接埠 — 僅通過 public-network 可達

  ai-api:
    networks:
      - internal-network

  postgres:
    networks:
      - db-network
    # 無主機連接埠 — 僅通過 db-network 可達
```

### 2.5 開發與生產的連接埠策略

```yaml
services:
  postgres:
    # --- 僅開發環境使用 — 生產環境必須移除 ---
    # ports:
    #   - "5432:5432"
    networks:
      - db-network
```

所有直接主機連接埠對應（後端 `8080`、postgres `5432`、rabbitmq `5672`/`15672`、ai-api `8000`）**預設註解掉**。取消註解會在檔案頭部印出 `SECURITY WARNING`，且必須在發布前恢復。

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **縱深防禦** | 即使前端 Nginx 被入侵，攻擊者也無法到達 PostgreSQL，因為 `public-network` 到 `db-network` 之間沒有網路路徑。 |
| **與 K8s 形態一致** | Docker Compose 堆疊的結構與帶 NetworkPolicies 的 K8s 部署鏡像對應：`public-network` ≈ ingress 暴露的命名空間，`internal-network` ≈ 叢集內部命名空間，`db-network` ≈ 受限命名空間。遷移到 K8s 無需重構服務通訊。 |
| **單一入口** | 每個 HTTP 請求都流經 `frontend:80`。CORS、速率限制、TLS 終止和 WAF 規則只需在 Nginx 中實現一次。 |
| **金鑰隔離** | PostgreSQL 密碼僅 `backend` 和 `postgres` 容器知曉。AI 服務永遠不會看到它。 |
| **本地安裝包安全性** | 作為 VirtualBox/VMware 虛擬機發布時，虛擬機的單一轉發連接埠（`80→80`）僅暴露 Nginx。主機 OS 不會意外繫結到 `5432` 並與開發者本地的 PostgreSQL 衝突。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **後端複雜度增加** | 後端必須管理三個網路介面並正確路由流量。配置錯誤（例如忘記接入 `db-network`）會導致連線失敗，比扁平網路更難除錯。 |
| **開發摩擦** | 想要直接用 pgAdmin 或 Postman 連接後端的開發者必須取消註解主機連接埠，並記得在提交前恢復。 |
| **多宿主 DNS 怪異行為** | 接入多個網路的容器無法預測地將自身主機名稱解析到哪個網路。後端必須使用顯式服務名稱（`postgres`、`rabbitmq`）而非 `localhost`。 |
| **無內建加密** | Docker bridge 網路提供 Layer-2 隔離，但不提供加密。後端與 postgres 之間的流量在鏈路上是明文。對於多主機部署，需要 overlay 網路加 TLS 或服務網格。 |

### 3.3 Risks / 風險與緩解

| 風險 | 緩解措施 |
|------|----------|
| 開發者意外提交取消註解了開發連接埠的 `docker-compose.yml` | **Git 忽略 + CI 檢查**：`docker-compose.yml` 被 gitignored（提交的是範例檔案）。CI 執行 `docker compose config`，若檢測到除 `frontend:80` 外的任何主機連接埠則建置失敗。 |
| 前端 `VITE_API_BASE_URL` 設為絕對 URL 繞過 Nginx | **建置時斷言**：前端 Dockerfile 檢查 `VITE_API_BASE_URL`；若以 `http` 開頭則建置失敗。文件明確警告不要使用絕對 URL。 |
| 後端容器逃逸危及所有層 | **執行時加固**：容器以非 root 執行（`USER 1000:1000`），唯讀根檔案系統（`read_only: true`），並丟棄所有 capabilities（`cap_drop: [ALL]`）。 |
| Docker bridge MTU 不匹配導致雲虛擬機靜默丟包 | **顯式 MTU**：每個網路宣告 `com.docker.network.driver.mtu: 1500` 以匹配標準乙太網路；雲覆蓋層 MTU 問題（例如 AWS VPC 9001 Jumbo Frames）在主機層面處理。 |

---

## 4. Compliance / 合規驗證

- **CI 連接埠掃描**：每個 PR 執行 `ci/check-compose-ports.sh`，解析 `docker-compose.yml` 並斷言只有 `frontend` 暴露主機連接埠。
- **安全審查**：每季審計 `docker-compose.yml` 和 `.env.example`，確保沒有新服務在未修訂 ADR 的情況下引入主機連接埠暴露。
- **滲透測試**：年度外部滲透測試包含網路分段驗證 — 確認從 `public-network` 中的容器執行 `nmap` 無法到達 `postgres:5432` 或 `rabbitmq:5672`。
- **文件同步**：每當服務到網路的對應發生變化時，必須重新生成 `docs/deployment/architecture.md` ASCII 架構圖。

---

## 5. Related / 相關決策

- ADR-0001 — 六邊形架構（後端作為單一閘道，與 Ports & Adapters 邊界控制一致）
- ADR-0002 — PostgreSQL + pgvector（`db-network` 隔離）
- ADR-0003 — RabbitMQ + Outbox（`internal-network` 隔離）
- ADR-0004 — Redis 快取與鎖（`internal-network` 隔離）
- ADR-0005 — Embedding 服務抽象（AI 服務位於 `internal-network`，僅通過後端可達）

---

## 6. Notes / 備註

> 三層模型是 **Purdue 工業控制系統安全模型** 在雲原生應用堆疊中的簡化版本：
> - 第一層（Public）≈ DMZ / 企業區
> - 第二層（Internal）≈ 製造區
> - 第三層（Database）≈ 安全區 / Level 3.5
>
> 對於企業 Kubernetes 部署，每個 Docker 網路對應到一個命名空間 + NetworkPolicy：
> | Docker 網路 | Kubernetes 等價物 |
> |-------------|---------------------|
> | `public-network` | `ingress-nginx` 命名空間，允許 ingress |
> | `internal-network` | `jobcopilot-app` 命名空間，禁止 ingress，允許 egress 到 db 命名空間 |
> | `db-network` | `jobcopilot-data` 命名空間，僅允許來自 `jobcopilot-app` 命名空間的 ingress |
>
> 後端的多網路接入對應到 Kubernetes 的 **Sidecar** 或 **Ambassador** 模式，其中閘道容器跨越信任邊界。

---

*End of ADR-0006*
