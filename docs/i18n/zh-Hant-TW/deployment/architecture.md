# Resume Assistant — Docker Compose 部署架構

> [English](../../../deployment/architecture.md) | [简体中文](../../../i18n/zh-Hans-CN/deployment/architecture.md)

## 1. 概述

Resume Assistant（智慧求職助手）是一個以**三層 Docker 網路架構**部署的 AI 驅動求職平台：面向公網的反向代理層、內部應用層以及隔離的資料庫層。

## 2. 網路架構

```
                         Internet
                            |
                            v
                    +---------------+
                    | Host:80 ->    |  <-- 唯一公網入口
                    | Nginx : 8080  |
                    |  (frontend)   |
                    +---------------+
                            |
            +---------------+---------------+
            |                               |
            v                               v
   +------------------+           +------------------+
   |  backend : 8080  |           |  ai-api   :8000  |
   |  (Spring Boot)   |<--------->|  (FastAPI)       |
   +------------------+           +------------------+
            |                               |
            |      +------------------+     |
            |      | rabbitmq : 5672  |<----+
            |      | (Message Queue)  |     |
            |      +------------------+     |
            |      +------------------+     |
            +----->|  redis   : 6379  |<----+
                   | (快取與鎖)       |
                   +------------------+
            |               |
            v               v
   +------------------+   +------------------+
   | postgres : 5432  |   |  ai-worker       |
   | (PostgreSQL +    |   |  (LightGBM)      |
   |  pgvector)       |   +------------------+
   +------------------+           |
                                  v
                          +------------------+
                          |  minio : 9000    |
                          |  (模型註冊表)    |
                          +------------------+
```

### 網路分層

| 網路 | 服務 | 外部暴露 | 用途 |
|------|------|----------|------|
| **公網（Public）** | `frontend`（Nginx）、`backend` | 主機 `${FRONTEND_HOST_PORT:-80}` 轉發到 `frontend:8080` | 所有 HTTP/HTTPS 流量的單一入口 |
| **內網（Internal）** | `backend`、`ai-api`、`ai-worker`、`rabbitmq`、`redis`、`minio` | 無（僅 Docker DNS） | 服務間透過容器名稱通信 |
| **資料庫網（Database）** | `backend`、`postgres` | 無（僅 Docker DNS） | 隔離的持續性資料儲存 |

> **開發範本說明**：目前 `docker-compose.yml` 預設只暴露前端。後端（`8080`）、AI 服務（`8000`）、PostgreSQL（`5432`）、RabbitMQ（`5672`）和 RabbitMQ Management（`15672`）的主機連接埠僅保留為已註解的開發除錯範例。在**生產部署**中，僅應暴露前端主機連接埠。

## 3. 服務清單

### 3.1 前端（Nginx + React）

| 屬性 | 值 |
|------|-----|
| **網路** | `public-network` |
| **主機連接埠** | `${FRONTEND_HOST_PORT:-80}:8080` |
| **職責** | 靜態單頁應用程式託管與所有 API 流量的反向代理。 |
| **安全說明** | Nginx 將 `/api/*` 代理至 `backend:8080`。環境變數 `VITE_API_BASE_URL` **必須為空或相對路徑**（例如 `/api`）。若設為絕對 URL（如 `http://backend:8080`），瀏覽器將直接連接後端，繞過 Nginx，破壞單入口安全模型。 |

### 3.2 後端（Spring Boot）

| 屬性 | 值 |
|------|-----|
| **網路** | `public-network`、`internal-network`、`db-network` |
| **主機連接埠** | 預設無；`8080:8080` 是已註解的開發除錯範例 |
| **職責** | REST API 閘道、JWT 身分驗證、CAPTCHA 驗證、業務邏輯編排、RabbitMQ 生產者。 |
| **安全說明** | 唯一跨越三層網路的服務。透過 Docker DNS 與 PostgreSQL（`postgres:5432`）和 RabbitMQ（`rabbitmq:5672`）通信。所有發往 `ai-service` 的出站 REST 請求均攜帶 `X-Internal-API-Key` 標頭。 |

### 3.3 AI API (FastAPI)

| 屬性 | 值 |
|------|-----|
| **網路** | `internal-network` |
| **主機連接埠** | 預設無；`8000:8000` 是已註解的開發除錯範例 |
| **職責** | 大型語言模型（LLM）推理、嵌入向量生成、履歷/職缺解析、職缺排序、適配度評分，以及自適應模型產物載入。 |
| **安全說明** | REST 端點 `/api/v1/ai/embeddings` 受 `X-Internal-API-Key` 中介軟體保護。MQ 消費者監聽四個佇列：`ai.queue.job.parse`、`ai.queue.resume.parse`、`ai.queue.conversation`、`ai.queue.job.rank`。不存取資料庫。 |

### 3.4 AI Worker (LightGBM)

| 屬性 | 值 |
|------|-----|
| **網路** | `internal-network` |
| **主機連接埠** | 無 |
| **職責** | 用於增量模型訓練的背景工作程式。從 `ai.queue.feedback` 消耗反饋並將訓練好的模型儲存到 MinIO。 |
| **安全說明** | 嚴格與 PostgreSQL 隔離。僅與 RabbitMQ、Redis 和 MinIO 通信。 |

### 3.5 PostgreSQL（含 pgvector）

| 屬性 | 值 |
|------|-----|
| **網路** | `db-network` |
| **主機連接埠** | 預設無；`5432:5432` 是已註解的開發除錯範例 |
| **職責** | 業務資料與嵌入向量的統一儲存。 |
| **安全說明** | 使用 `pgvector` 擴充功能進行相似度檢索。僅能從 Docker 網路內的 `backend` 存取。即使具備網路隔離，強密碼 `POSTGRES_PASSWORD` 仍是縱深防禦的必備措施。 |

### 3.6 RabbitMQ（Management）

| 屬性 | 值 |
|------|-----|
| **網路** | `internal-network` |
| **主機連接埠** | 預設無；`5672:5672` 和 `15672:15672` 是已註解的開發除錯範例 |
| **職責** | 後端、AI API 與 AI Worker 之間的非同步訊息代理（Outbox 模式，訊息佇列）。 |
| **安全說明** | 透過環境變數覆蓋預設的 `guest/guest` 憑證。管理面板（`:15672`）絕不應暴露於公網；透過 SSH 隧道存取：`ssh -L 15672:localhost:15672 <host>`。訊息大小限制設為 10 MB（`max_message_size 10485760`），以容納向量和履歷摘要。 |

### 3.7 Redis（快取與鎖）

| 屬性 | 值 |
|------|-----|
| **網路** | `internal-network` |
| **主機連接埠** | 生產環境無 |
| **職責** | 分散式狀態儲存：CAPTCHA 挑戰/token、驗證碼、對話流橋接、增量模型統計、去重集合、分散式鎖（ShedLock）。 |
| **安全說明** | 無外部存取。開發環境密碼認證可選（`REDIS_PASSWORD` 可為空），生產環境建議啟用。資料透過 `redis-data` 命名卷持久化。 |

### 3.8 MinIO (模型註冊表)

| 屬性 | 值 |
|------|-----|
| **網路** | `internal-network` |
| **主機連接埠** | 生產環境無 |
| **職責** | 儲存已訓練的 LightGBM 模型產物的物件儲存。 |
| **安全說明** | 無外部存取。專供 `ai-worker` (寫入) 和 `ai-api` (讀取) 使用。 |

## 4. 縱深防禦（Defense in Depth）

本部署實作了五層獨立的安全層。攻破一層不會自動導致下一層失守。

### 第一層：網路隔離

僅 `frontend` 服務透過主機 `${FRONTEND_HOST_PORT:-80}` 面向公網暴露，並轉發到容器內 Nginx 的 `8080` 連接埠。其他所有服務透過 Docker 內部 DNS（`<service-name>`）通信。對主機的外部連接埠掃描應只能發現配置的前端主機連接埠。

### 第二層：應用層 API 金鑰

`INTERNAL_API_KEY` 環境變數在後端與 AI 服務之間共享。後端發往 AI 服務的每個 REST 請求均攜帶 `X-Internal-API-Key` 標頭。AI 服務中介軟體對缺失或不匹配的金鑰返回 HTTP 401。

> **安全模型**：即使攻擊者獲得內網 Docker 網路的存取權限，沒有金鑰也無法呼叫 LLM 嵌入端點。

### 第三層：JWT 身分驗證

所有面向使用者的 API 呼叫（註冊、登入、履歷上傳、職缺匹配）均攜帶簽章 JWT，置於 `Authorization: Bearer <token>` 標頭中。簽章金鑰（`JWT_SECRET`）僅後端知曉。金鑰輪換或變更將強制所有使用者重新登入。

### 第四層：RabbitMQ 憑證

AMQP 連線需要使用者名稱和密碼。預設的 `guest/guest` 透過環境變數覆蓋。即使某個容器被攻破，存取訊息代理仍需要獨立的憑證。

### 第五層：人機驗證（CAPTCHA）

所有認證端點（註冊、登入）均要求有效的 CAPTCHA 挑戰-回應。後端維護前綴隔離的 Redis 快取（String 儲存挑戰/token，Sorted Set 儲存 IP 速率限制滑動視窗）用於儲存挑戰和一次性 token，並實施基於 IP 的速率限制（每分鐘 20 次請求）。即使攻擊者繞過網路隔離並持有有效憑證，也無法在不解決 CAPTCHA 挑戰的情況下以程式設計方式完成認證。

## 5. 快速開始

```bash
# 1. 複製 Compose 範本
cp docker-compose.yml.example docker-compose.yml

# 2. 複製環境變數範本
cp .env.example .env

# 3. 編輯 .env，替換所有 [replace-me] 佔位符
vim .env

# 4. 產生強 JWT 金鑰（48 位元組 = 64 個 base64 字元）
openssl rand -base64 48
# 將輸出貼到 .env 的 JWT_SECRET 中

# 5. 產生內部 API 金鑰（32 位元組 = 44 個 base64 字元）
openssl rand -base64 32
# 將輸出貼到 .env 的 INTERNAL_API_KEY 中
#（後端和 AI 服務必須使用完全相同的值）

# 6. 啟動所有服務
docker compose up -d

# 7. 驗證容器健康狀態
docker compose ps

# 8. 檢查前端健康端點
curl -f http://localhost/health
```

第 8 步的預期輸出：`HTTP 200 OK` 及簡短的健康狀態正文。

## 6. 故障排查

### 連接埠 80 已被占用

**現象**：`docker compose up` 失敗，報錯 `bind: address already in use`。

**解決**：修改 `docker-compose.yml` 中的前端主機連接埠：

```yaml
ports:
  - "8081:8080"   # 或主機上任意閒置連接埠
```

然後透過 `http://localhost:8081` 存取應用程式。

### RabbitMQ 管理面板無法存取

**現象**：瀏覽器無法連線 `http://localhost:15672`。

**解決**：生產環境中，管理面板**故意**不對外暴露。請使用 SSH 隧道：

```bash
ssh -L 15672:localhost:15672 user@your-server
# 然後在本機瀏覽器開啟 http://localhost:15672
```

### AI 服務返回 401 Unauthorized

**現象**：後端日誌顯示呼叫嵌入端點時返回 `401 Unauthorized: invalid or missing internal API key`。

**解決**：確保 `INTERNAL_API_KEY` 在後端和 AI 服務的環境中設定為**完全相同的值**。驗證方式：

```bash
docker compose exec backend env | grep INTERNAL_API_KEY
docker compose exec ai-service env | grep INTERNAL_API_KEY
```

### 資料庫連線被拒絕

**現象**：從主機執行 `psql` 時報錯 `could not connect to server: Connection refused`。

**解決**：PostgreSQL 位於隔離的內網 Docker 網路中，生產環境**不**向主機暴露連接埠 `5432`。這是預期行為。如需存取資料庫，請進入容器：

```bash
docker compose exec postgres psql -U resume_user -d resume_assistant
```
