<!-- 語言切換 -->
> [English](../../../architecture/Architecture.md) | [简体中文](../../zh-Hans-CN/architecture/Architecture.md) | **繁體中文**

# JobCopilot 架構

## 文件資訊

| 欄位 | 內容 |
|------|------|
| 專案 | JobCopilot |
| 文件 | 系統架構 |
| 版本 | 2.0.0 |
| 狀態 | 持續更新 |
| 最近更新 | 2026-06-05 |
| 讀者 | 維護者、貢獻者、部署人員 |

## 概覽

JobCopilot 是一個容器化的 AI 求職平台，由 React 前端、Java Spring Boot 後端、Python FastAPI AI 服務、AI Worker、PostgreSQL + pgvector、RabbitMQ、Redis 和 MinIO 組成。

架構目標是保持清晰服務邊界、非同步處理耗時 AI 任務、透過 LiteLLM 支援可替換模型提供商，並透過 Docker Compose 提供預設只暴露前端閘道的本地部署方式。

## 架構原則

| 原則 | 設計選擇 |
|------|----------|
| 清晰職責邊界 | 前端負責 UI，後端負責業務工作流和持久化，AI 服務負責模型呼叫和排序邏輯 |
| 領域優先後端 | 後端模組遵循 DDD 和六邊形架構：`types`、`domain`、`api`、`infrastructure`、`trigger`、`app` |
| 耗時 AI 任務非同步化 | RabbitMQ 承載解析、排序、對話和回饋任務 |
| 統一向量儲存 | PostgreSQL 同時保存業務實體和 pgvector 嵌入向量 |
| AI 提供商中立 | 透過環境變數配置 LiteLLM 相容模型名稱和金鑰 |
| 執行時分層隔離 | Docker 網路隔離 public、internal 和 database 層 |
| 易於本地復現 | `.env.example` 和 `docker-compose.yml` 提供可復現本地堆疊 |

## 執行拓撲

```text
Browser
  |
  | HTTP :${FRONTEND_HOST_PORT:-80}
  v
Frontend container
  React 靜態應用 + Nginx 反向代理
  |
  | HTTP /api, /health
  v
Backend container
  Spring Boot API、認證、領域工作流、向量持久化
  |-- JDBC --------> PostgreSQL + pgvector
  |-- AMQP --------> RabbitMQ --------> AI worker container
  |-- HTTP --------> AI Service container
  |-- Redis -------> Redis
  `-- Local files -> shared upload volume

AI Service / AI worker
  |-- LiteLLM 相容提供商：解析、嵌入、排序、對話
  |-- 後端內部 API：向量寫入和基線特徵讀取
  |-- Redis：回饋緩衝、分散式鎖、模型重載 Pub/Sub
  `-- MinIO：LightGBM 模型產物註冊表
```

## 元件

| 元件 | 技術 | 網路暴露 | 職責 |
|------|------|----------|------|
| 前端 / 閘道 | React 19、Vite 7、Nginx | 主機 `${FRONTEND_HOST_PORT:-80}` -> 容器 `8080` | 提供 UI，反向代理後端 API 和健康檢查 |
| 後端 | Java 21、Spring Boot 3.5 | 內部 `8080`；可選開發連接埠映射 | REST API、認證、領域工作流、交易、持久化、MQ 發布/消費 |
| AI Service | Python 3.11、FastAPI、LiteLLM | 內部 `8000`；可選開發連接埠映射 | 嵌入、解析、排序和對話相關同步端點 |
| AI Worker | Python 3.11、RabbitMQ 消費者、LightGBM | 內部工作程序 | 非同步任務處理、回饋採集、增量模型訓練 |
| PostgreSQL | PostgreSQL 15 + pgvector | `db-network` 內部 `5432` | 業務表和向量表 |
| RabbitMQ | RabbitMQ 3 management 映像 | 內部 `5672`；管理 UI 預設關閉 | 持久化佇列傳輸和 DLQ 支援 |
| Redis | Redis 7 | 內部 `6379` | CAPTCHA 狀態、分散式鎖、回饋緩衝、模型重載 Pub/Sub |
| MinIO | S3 相容物件儲存 | 內部 `9000`；控制台預設不暴露 | LightGBM 模型產物和元資料 |

## 後端架構

後端是 Maven 多模組 Spring Boot 系統。

| 模組 | 角色 | 依賴方向 |
|------|------|----------|
| `types` | 共享列舉、值型別、常數 | 不依賴其他專案模組 |
| `domain` | 實體、值物件、連接埠、領域服務 | 依賴 `types` |
| `api` | DTO、命令、查詢、外觀介面 | 依賴 `domain`、`types` |
| `infrastructure` | JPA、Redis、RabbitMQ、儲存、外部服務適配器 | 實作 domain/API 連接埠 |
| `trigger` | REST 控制器、WebSocket 端點、事件/MQ 監聽器 | 呼叫應用/API 介面 |
| `app` | Spring Boot 啟動、配置、應用服務、排程器 | 裝配所有模組 |

後端規則：

- 領域程式碼不依賴 Spring、持久化、HTTP 或訊息中介軟體 API。
- 應用服務負責交易邊界。
- 資料庫交易內不執行網路 I/O。
- 非同步副作用應透過 RabbitMQ 和適用的 outbox 風格可靠性機制處理。
- 向量維度由 `LLM_EMBEDDING_MODEL_DIMENSION` 控制，必須與嵌入模型匹配。

## AI 服務架構

Python 服務同時包含同步 API 和背景 Worker 邏輯。

| 區域 | 路徑 | 職責 |
|------|------|------|
| API | `ai-service/app/main.py`、`app/api/` | FastAPI 端點、健康檢查、模型管理器 |
| 領域 | `ai-service/app/domain/` | AI 領域抽象和共享邏輯 |
| 基礎設施 | `ai-service/app/infrastructure/` | 後端內部 API 客戶端、Redis 客戶端、MinIO 客戶端 |
| 服務 | `ai-service/app/services/` | LLM 呼叫、嵌入、解析、匹配、排序 |
| MQ | `ai-service/app/mq/`、`app/worker/consumers/` | RabbitMQ 消費者和訊息處理 |
| Worker | `ai-service/app/worker_main.py`、`app/worker/` | 回饋處理和 LightGBM 重新訓練 |

AI 設計說明：

- LiteLLM 是文字、視覺和嵌入模型的統一抽象。
- 嵌入輸出長度會校驗 `LLM_EMBEDDING_MODEL_DIMENSION`。
- AI Worker 在 Redis 中緩衝回饋樣本後再訓練。
- 訓練後的 LightGBM 產物上傳到 MinIO，並透過 Redis Pub/Sub 通知重載。
- AI 服務透過後端內部 API 寫入向量和讀取基線特徵。

## 資料架構

| 儲存 | 內容 | 說明 |
|------|------|------|
| PostgreSQL | 使用者、履歷、職位、對話、申請記錄、outbox/任務狀態 | 業務資料事實來源 |
| pgvector | 履歷和職位嵌入向量 | 用於語義召回和相似度搜尋 |
| Redis | CAPTCHA challenge/token/rate-limit 狀態、AI 回饋緩衝、鎖、模型重載事件 | Compose 中需要 `REDIS_PASSWORD` |
| RabbitMQ | AI 請求/回應佇列、回饋佇列、DLQ | 預設僅內部存取 |
| 本地上傳卷 | 預設 Compose 配置下的上傳履歷檔案 | 後端 Compose 配置使用 `STORAGE_TYPE=local` |
| MinIO | AI 模型產物和元資料 | 由 AI Worker/模型管理器使用；自訂部署中也可用於履歷儲存 |

## 主要流程

### 履歷上傳和解析

```text
User -> Frontend -> Backend
Backend -> PostgreSQL: 保存履歷元資料和版本
Backend -> Local upload volume: 保存檔案
Backend -> RabbitMQ: 發布解析請求
AI worker -> LiteLLM provider: 解析文件內容
AI worker -> Backend: 返回結構化資料和向量
Backend -> PostgreSQL/pgvector: 持久化解析資料和嵌入
Frontend -> Backend: 輪詢或取得更新後的履歷狀態
```

### 職位匹配

```text
Frontend -> Backend: 請求匹配結果
Backend -> PostgreSQL/pgvector: 語義召回
Backend -> AI Service or RabbitMQ: 排序/解釋任務
AI service -> LiteLLM provider: 排序或解釋匹配結果
Backend -> Frontend: 返回排序後的職位和匹配元資料
```

### 回饋與增量訓練

```text
User scoring behavior -> Backend
Backend -> RabbitMQ: 發布回饋事件
AI worker -> Redis: 緩衝標註樣本
AI worker -> Backend internal API: 取得基線特徵
AI worker -> LightGBM: 滿足閾值且取得鎖後訓練
AI worker -> MinIO: 上傳模型產物和 latest 元資料
AI worker -> Redis Pub/Sub: 發布模型重載事件
AI Service model manager -> MinIO: 載入最新模型
```

## 部署拓撲

Docker Compose 定義三層網路：

| 網路 | 成員 | 用途 |
|------|------|------|
| `public-network` | `frontend`、`backend` | 瀏覽器入口閘道存取後端 |
| `internal-network` | `backend`、`ai-service`、`ai-worker`、`rabbitmq`、`redis`、`minio` | 內部服務通訊 |
| `db-network` | `backend`、`postgres` | 資料庫存取與其他服務隔離 |

預設只有 `frontend` 映射主機連接埠。後端、AI Service、RabbitMQ 管理端、Redis、PostgreSQL 和 MinIO 都保持內部存取，除非明確開啟開發用連接埠映射。

## 安全邊界

- 金鑰從 `.env` 載入，`.env.example` 記錄本地所需值。
- 共享或類生產環境必須更換 `JWT_SECRET`、`INTERNAL_API_KEY`、資料庫憑證、RabbitMQ 憑證、Redis 密碼和 MinIO 金鑰。
- 後端負責使用者側工作流的認證和授權。
- AI 服務應透過後端內部 API 存取業務資料，而不是直接存取資料庫。
- Docker 網路隔離預設阻止主機直接存取資料平面服務。
- 上傳檔案和模型產物與應用程式碼分離儲存。

## 擴展點

| 變更 | 預期擴展點 |
|------|------------|
| 增加儲存後端 | 實作後端儲存適配器並設定 `STORAGE_TYPE` |
| 增加 LLM 提供商 | 在 `.env` 中配置 LiteLLM 模型名和提供商金鑰 |
| 增加 AI 任務 | 增加 RabbitMQ 訊息契約、後端發布/監聽邏輯、AI 消費者 |
| 增加領域工作流 | 增加領域模型/連接埠、應用服務、基礎設施適配器、觸發器端點 |
| 增加可觀測性 | 在 Compose 或部署清單中擴展 metrics/logging 服務 |

## 相關文件

- [Docker 部署](../deployment/DOCKER_DEPLOY.md)
- [環境變數](../deployment/environment-variables.md)
- [交易策略](../../../transactional-strategy.md)
- [分支和提交規範](../../../BRANCHING_AND_COMMITS.md)
- [架構決策記錄](../../../adr/)
