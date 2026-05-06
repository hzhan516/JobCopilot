# Resume Assistant — 環境變數參考手冊

> [English](../../../deployment/environment-variables.md) | [简体中文](../../../i18n/zh-Hans-CN/deployment/environment-variables.md)

本文件描述 Resume Assistant 技術堆疊使用的所有環境變數。變數依功能區域組織，與 `.env.example` 中的註解區塊一一對應。

> **快速提示**：編輯前先執行 `cp .env.example .env`。切勿將 `.env` 提交到版本控制。

---

## 目錄

- [Docker Compose 設定](#docker-compose-設定)
- [A. 資料庫 / PostgreSQL](#a-資料庫--postgresql)
- [B. 訊息佇列 / RabbitMQ](#b-訊息佇列--rabbitmq)
- [C. 身分驗證 / JWT](#c-身分驗證--jwt)
- [D. 前端 / Web 應用程式](#d-前端--web-應用程式)
- [E. Spring Boot / 後端](#e-spring-boot--後端)
- [F. AI 供應商金鑰](#f-ai-供應商金鑰)
- [G. 模型參數](#g-模型參數)
- [H. AI 服務日誌](#h-ai-服務日誌)
- [I. Vertex AI 設定](#i-vertex-ai-設定)
- [J. 內部 API 金鑰](#j-內部-api-金鑰)

---

## Docker Compose 設定

以下值**未**在 `.env.example` 中定義，但可透過 `docker-compose.yml` 設定。它們影響容器命名與主機連接埠繫結。

### `COMPOSE_PROJECT_NAME`

| 欄位 | 值 |
|------|-----|
| **用途** | 容器名稱、資料卷與網路的前綴。支援在同一 Docker 主機上執行多個獨立實例。 |
| **預設值** | 專案根目錄的目錄名稱（例如 `ser594_ai_prject`） |
| **有效取值** | 任意小寫字母數字字串，可含連字號/底線 |
| **安全說明** | 使用不同的專案名稱可防止開發、預備和生產環境之間的意外交叉污染。 |
| **常見錯誤** | 對同一儲存庫的兩個複本使用相同的專案名稱，導致連接埠和資料卷衝突。 |

### `FRONTEND_HOST_PORT`

| 欄位 | 值 |
|------|-----|
| **用途** | 映射到 Nginx 容器連接埠 `80` 的主機連接埠。 |
| **預設值** | `8081`（在 `docker-compose.yml.example` 中） |
| **有效取值** | 主機上任意閒置的 TCP 連接埠（`80`、`8080`、`8081`、`3000` 等） |
| **安全說明** | 生產環境中應設為 `80`（或在 TLS 終止器後設為 `443`）。請勿將後端/AI/資料庫連接埠暴露給主機。 |
| **常見錯誤** | 在 macOS/Linux 上未使用 `sudo` 就將此值設為 `80`，因為小於 1024 的連接埠需要 root 權限。本地開發請使用 `8081`。 |

### `STORAGE_TYPE`

| 欄位 | 值 |
|------|-----|
| **用途** | 決定上傳的履歷檔案持續性儲存位置。 |
| **預設值** | `local`（在 `docker-compose.yml` 中硬編碼） |
| **有效取值** | `local`（Docker 卷）、`minio`（自托管 S3 相容）、`s3`、`oss` |
| **安全說明** | `local` 將檔案儲存在命名 Docker 卷（`shared-storage`）中。對於多主機部署，請切換到 `minio` 或 `s3`，以便所有副本共享同一個物件儲存。 |
| **常見錯誤** | 在未設定 `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY` 和 `MINIO_SECRET_KEY` 的情況下將 `STORAGE_TYPE` 設為 `minio`，導致執行時檔案上傳失敗。 |

---

## A. 資料庫 / PostgreSQL

### `POSTGRES_DB`

| 欄位 | 值 |
|------|-----|
| **用途** | 容器首次啟動時建立的預設資料庫名稱。 |
| **預設值** | `resume_assistant` |
| **有效取值** | 任意有效的 PostgreSQL 識別項 |
| **安全說明** | 資料庫名稱不是機密，但偏離預設值可使自動化掃描稍微困難。 |
| **常見錯誤** | 在資料卷已初始化後重新命名此變數無效。`docker-entrypoint-initdb.d` 僅在資料目錄為空時首次執行。 |

### `POSTGRES_USER`

| 欄位 | 值 |
|------|-----|
| **用途** | PostgreSQL 實例的超級使用者名稱。 |
| **預設值** | `resume_user` |
| **有效取值** | 任意有效的 PostgreSQL 使用者名稱 |
| **安全說明** | 避免使用 `postgres` 作為使用者名稱。生產環境中請使用具有有限權限的專用應用程式帳戶。 |
| **常見錯誤** | 與 `POSTGRES_DB` 相同——初始化後變更需要重建資料卷。 |

### `POSTGRES_PASSWORD`

| 欄位 | 值 |
|------|-----|
| **用途** | `POSTGRES_USER` 的密碼。 |
| **預設值** | `resume_pass` |
| **有效取值** | 任意字串；建議長度 ≥ 16 個字元 |
| **安全說明** | 即使 PostgreSQL 位於隔離的 Docker 網路中，強密碼仍是縱深防禦的必備措施。如果容器被攻破並獲得網路存取權限，弱憑證將允許輕易存取資料庫。 |
| **常見錯誤** | 在任何非本地環境中保留預設的 `resume_pass`。 |

### `POSTGRES_HOST`

| 欄位 | 值 |
|------|-----|
| **用途** | 後端連線 PostgreSQL 時使用的主機名稱。 |
| **預設值** | `postgres`（Docker 服務名稱） |
| **有效取值** | Docker 服務名稱、容器 IP 或外部主機名稱 |
| **安全說明** | 使用 Docker 服務名稱（`postgres`）可確保流量不會離開內部橋接網路。 |
| **常見錯誤** | 在容器內部將其設為 `localhost`——容器不共享主機的回環介面。 |

### `POSTGRES_PORT`

| 欄位 | 值 |
|------|-----|
| **用途** | PostgreSQL 監聽連接埠。 |
| **預設值** | `5432` |
| **有效取值** | `1024–65535` |
| **安全說明** | 非標準連接埠帶來的安全收益極小（透過隱蔽實現安全）。網路隔離才是主要防禦手段。 |
| **常見錯誤** | 變更此值但未同步更新 `docker-compose.yml` 中的 `ports` 映射。 |

---

## B. 訊息佇列 / RabbitMQ

### `RABBITMQ_HOST`

| 欄位 | 值 |
|------|-----|
| **用途** | 後端和 AI 服務連線 RabbitMQ 時使用的主機名稱。 |
| **預設值** | `rabbitmq`（Docker 服務名稱） |
| **有效取值** | Docker 服務名稱或外部主機名稱 |
| **安全說明** | 內部 Docker DNS 解析使 AMQP 流量不經過主機網路。 |
| **常見錯誤** | 在容器內部使用 `localhost`。 |

### `RABBITMQ_PORT`

| 欄位 | 值 |
|------|-----|
| **用途** | AMQP 協定連接埠。 |
| **預設值** | `5672` |
| **有效取值** | `5672`（標準）、`5671`（TLS） |
| **安全說明** | 生產環境中透過切換到連接埠 `5671` 並掛載 TLS 憑證來啟用 TLS（`amqps://`）。 |
| **常見錯誤** | 將其與管理面板連接埠（`15672`）混淆。 |

### `RABBITMQ_USERNAME`

| 欄位 | 值 |
|------|-----|
| **用途** | AMQP 認證使用者名稱。 |
| **預設值** | `guest` |
| **有效取值** | 任意字串 |
| **安全說明** | `guest` 帳戶在 RabbitMQ 中預設禁用於遠端連線。生產環境中務必覆蓋此值。 |
| **常見錯誤** | 在生產部署中使用 `guest`。 |

### `RABBITMQ_PASSWORD`

| 欄位 | 值 |
|------|-----|
| **用途** | AMQP 認證密碼。 |
| **預設值** | `guest` |
| **有效取值** | 任意字串；建議長度 ≥ 16 個字元 |
| **安全說明** | 這是縱深防禦的第四層。即使攻擊者攻破網路，仍需要有效的 MQ 憑證才能發布或消費訊息。 |
| **常見錯誤** | 與 `POSTGRES_PASSWORD` 或 `JWT_SECRET` 使用相同的密碼。請獨立輪換。 |

---

## C. 身分驗證 / JWT

### `JWT_SECRET`

| 欄位 | 值 |
|------|-----|
| **用途** | JSON Web Token（JWT）的對稱簽章金鑰。用於簽章和驗證所有使用者身分驗證令牌。 |
| **預設值** | `change-this-to-a-long-random-secret-at-least-32-characters` |
| **有效取值** | Base64 或純文字字串；**最少 32 位元組（256 位元）** |
| **安全說明** | 這是整個技術堆疊中最關鍵的機密。任何知曉此值的人都可以偽造身分驗證令牌並冒充任意使用者（包括管理員）。生產環境中請將其儲存在金鑰管理器中（Docker secrets、HashiCorp Vault、AWS Secrets Manager 等）。 |
| **常見錯誤** | <ul><li>使用簡短的人類可讀字串，如 `my-secret` 或 `password123`。</li><li>將 `.env` 提交到 Git。</li><li>在開發/預備/生產環境中使用相同的金鑰。</li><li>未做規劃就輪換金鑰——**所有活躍使用者將被迫立即重新登入**，因為其現有令牌將無法通過驗證。</li></ul> |

**推薦產生方式：**

```bash
# 48 位元組 = 64 個 base64 字元
openssl rand -base64 48
```

---

## D. 前端 / Web 應用程式

### `VITE_GOOGLE_CLIENT_ID`

| 欄位 | 值 |
|------|-----|
| **用途** | 前端「使用 Google 登入」的 Google OAuth 2.0 用戶端 ID。 |
| **預設值** | `your-google-oauth-client-id.apps.googleusercontent.com` |
| **有效取值** | Google Cloud Console 中有效的 OAuth 2.0 用戶端 ID |
| **安全說明** | 這是公開的用戶端 ID（不是金鑰）。它會被嵌入到編譯後的 JavaScript 套件中。請在 Google Cloud Console 中將授權的 JavaScript 來源限制為您的生產網域。 |
| **常見錯誤** | <ul><li>本地開發時未將 `http://localhost` 新增到授權來源。</li><li>將其與用戶端密鑰混淆（OAuth 2.0 隱式/帶 PKCE 的授權碼流程不需要用戶端密鑰）。</li></ul> |

### `VITE_API_BASE_URL`

| 欄位 | 值 |
|------|-----|
| **用途** | 所有前端 API 呼叫的基礎 URL 前綴。 |
| **預設值** | *（空）* |
| **有效取值** | 空字串、`/api` 或以 `/` 開頭的相對路徑 |
| **安全說明** | **關鍵警告**：此值必須為相對路徑或空。當前端在 Nginx 後執行時，API 呼叫會自動加上前綴並代理到後端。若設為絕對 URL（例如 `http://localhost:8080/api`），瀏覽器將直接連線後端，繞過 Nginx，暴露後端連接埠，破壞 CORS 和單入口安全模型。 |
| **常見錯誤** | <ul><li>本地開發時將 `VITE_API_BASE_URL` 設為 `http://localhost:8080/api`——這會將後端位址洩露給使用者，且在生產環境中會失敗。</li><li>將其設為內部 Docker 主機名稱，如 `http://backend:8080`——瀏覽器無法解析 Docker 服務名稱。</li></ul> |

---

## E. Spring Boot / 後端

### `SPRING_PROFILES_ACTIVE`

| 欄位 | 值 |
|------|-----|
| **用途** | 啟用 Spring Boot 設定檔（Profile）。 |
| **預設值** | `dev` |
| **有效取值** | `dev`、`prod`、`test` |
| **安全說明** | `dev` 設定檔停用 Flyway、使用 24 小時 JWT 過期時間，並啟用詳細錯誤訊息。**絕不要在生產環境中使用 `dev`。** `prod` 設定檔啟用 Flyway 驗證、設定 1 小時 JWT 過期時間，並隱藏 API 回應中的堆疊追蹤。 |
| **常見錯誤** | 生產部署時仍使用 `SPRING_PROFILES_ACTIVE=dev`。 |

---

## F. AI 供應商金鑰

您只需設定**一個**供應商。選擇由 `LLM_TEXT_MODEL`、`LLM_VISION_MODEL` 和 `LLM_EMBEDDING_MODEL` 中的前綴決定。

### `GEMINI_API_KEY`

| 欄位 | 值 |
|------|-----|
| **用途** | Google AI Studio 的 API 金鑰（透過 LiteLLM 使用 Gemini 模型）。 |
| **預設值** | `[replace-with-your-gemini-api-key]` |
| **有效取值** | [Google AI Studio](https://aistudio.google.com/app/apikey) 中有效的 Gemini API 金鑰 |
| **安全說明** | Gemini 提供免費額度。免費 tier 不需要信用卡或 GCP 計費。請將此金鑰視為機密——任何獲得它的人都可以消耗您的額度。 |
| **常見錯誤** | <ul><li>將金鑰貼到 `.env` 時帶有周圍空白或引號。</li><li>使用 GCP 服務帳號金鑰代替 AI Studio API 金鑰。</li></ul> |

### `OPENAI_API_KEY`

| 欄位 | 值 |
|------|-----|
| **用途** | 透過 LiteLLM 使用 OpenAI 模型（GPT-4o 等）的 API 金鑰。 |
| **預設值** | *（空）* |
| **有效取值** | 以 `sk-` 開頭的有效 OpenAI API 金鑰 |
| **安全說明** | OpenAI 是付費服務。請設定消費限額並監控使用儀表板，以避免意外帳單。 |
| **常見錯誤** | 同時設定此變數和 `GEMINI_API_KEY`。LiteLLM 將使用與模型前綴匹配的供應商，但保留多個金鑰會增加攻擊面。 |

### `ANTHROPIC_API_KEY`

| 欄位 | 值 |
|------|-----|
| **用途** | 透過 LiteLLM 使用 Anthropic Claude 模型的 API 金鑰。 |
| **預設值** | *（空）* |
| **有效取值** | 有效的 Anthropic API 金鑰 |
| **安全說明** | Claude 模型通常比 Gemini Flash 更昂貴。切換前請評估成本。 |
| **常見錯誤** | 切換供應商時忘記將模型前綴從 `gemini/` 更新為 `anthropic/`。 |

### `GROQ_API_KEY`

| 欄位 | 值 |
|------|-----|
| **用途** | 透過 LiteLLM 使用 Groq（快速推理）模型的 API 金鑰。 |
| **預設值** | *（空）* |
| **有效取值** | 有效的 Groq API 金鑰 |
| **安全說明** | Groq 提供極低延遲，但模型選擇有限。 |
| **常見錯誤** | 期望 Groq 提供嵌入支援——大多數 Groq 模型僅支援文字生成。 |

### `OLLAMA_API_BASE`

| 欄位 | 值 |
|------|-----|
| **用途** | 本地 Ollama 實例的基礎 URL（自托管開源模型）。 |
| **預設值** | `http://localhost:11434` |
| **有效取值** | AI 服務容器可達的任意 HTTP URL |
| **安全說明** | AI 服務容器內的 `localhost:11434` 指的是容器本身，而非 Docker 主機。要連線主機，請使用 `http://host.docker.internal:11434`（Docker Desktop）或主機的區域網路 IP。 |
| **常見錯誤** | <ul><li>保留預設的 `localhost:11434`，然後疑惑 AI 服務為何無法連線。</li><li>啟動技術堆疊前未拉取模型（`ollama pull llama3`）。</li></ul> |

---

## G. 模型參數

### `LLM_TEXT_MODEL`

| 欄位 | 值 |
|------|-----|
| **用途** | 通用文字生成（職缺解析、履歷最佳化、對話）的模型識別項。 |
| **預設值** | `gemini/gemini-2.5-flash` |
| **有效取值** | 任意 LiteLLM 支援的帶供應商前綴的模型字串，例如 `gemini/gemini-2.5-flash`、`openai/gpt-4o-mini`、`vertex_ai/gemini-2.5-flash` |
| **安全說明** | 模型名稱不是機密，但未經預算就切換到更昂貴的模型（例如 `gpt-4o`）可能導致意外成本。 |
| **常見錯誤** | 使用與設定的 API 金鑰不匹配的模型前綴（例如使用 `GEMINI_API_KEY` 時卻用 `openai/` 前綴）。 |

### `LLM_VISION_MODEL`

| 欄位 | 值 |
|------|-----|
| **用途** | 視覺/多模態任務（履歷圖片解析）的模型識別項。 |
| **預設值** | `gemini/gemini-2.5-flash` |
| **有效取值** | 任意支援視覺輸入的 LiteLLM 模型 |
| **安全說明** | 視覺模型通常按 token 計費更貴。請確保所選模型支援圖像輸入。 |
| **常見錯誤** | 為視覺任務選擇純文字模型（例如 `text-embedding-ada-002`）。 |

### `LLM_EMBEDDING_MODEL`

| 欄位 | 值 |
|------|-----|
| **用途** | 生成文字嵌入（向量檢索）的模型識別項。 |
| **預設值** | `gemini/gemini-embedding-001` |
| **有效取值** | 任意 LiteLLM 支援的嵌入模型 |
| **安全說明** | 嵌入模型的輸出維度必須與 `LLM_EMBEDDING_MODEL_DIMENSION` 匹配。 |
| **常見錯誤** | 切換嵌入模型時未更新維度，導致 PostgreSQL 中向量插入失敗。 |

### `LLM_EMBEDDING_MODEL_DIMENSION`

| 欄位 | 值 |
|------|-----|
| **用途** | 所選嵌入模型的輸出維度。必須與模型實際產生的向量大小匹配。 |
| **預設值** | `1536` |
| **有效取值** | 與模型輸出維度匹配的正整數 |
| **安全說明** | 維度不正確會導致執行時 SQL 錯誤（插入 `pgvector` 欄位時失敗），但不會造成安全漏洞。 |
| **常見錯誤** | <ul><li>將 `LLM_EMBEDDING_MODEL` 改為 `openai/text-embedding-3-large`（3072 維）卻保留 `LLM_EMBEDDING_MODEL_DIMENSION=1536`。</li><li>變更維度後忘記重建 PostgreSQL 資料卷——現有向量欄位保留舊維度，將拒絕新插入。</li></ul> |

**維度對照表：**

| 模型 | 維度 |
|------|------|
| `gemini/gemini-embedding-001` | `768` |
| `openai/text-embedding-ada-002` | `1536` |
| `openai/text-embedding-3-small` | `1536` |
| `openai/text-embedding-3-large` | `3072` |
| `sentence-transformers/all-MiniLM-L6-v2` | `384` |

> **注意**：`.env.example` 中的預設值 `1536` 對應 OpenAI Ada-002。若使用 Gemini 嵌入，請改為 `768`。

### `LLM_TEMPERATURE`

| 欄位 | 值 |
|------|-----|
| **用途** | 控制 LLM 輸出的隨機性（創造性）。 |
| **預設值** | `0.1` |
| **有效取值** | `0.0` 到 `2.0` |
| **安全說明** | 較低的值產生更確定的輸出，對於結構化 JSON 提取（履歷解析、職缺匹配）更安全。較高的值增加幻覺風險。 |
| **常見錯誤** | 為結構化提取任務設定 `1.0` 或更高，導致無效的 JSON 回應，破壞下游解析器。 |

### `LLM_REQUEST_TIMEOUT_SECONDS`

| 欄位 | 值 |
|------|-----|
| **用途** | 每次 LiteLLM 請求（文字、視覺、嵌入）的最大等待時間。 |
| **預設值** | `60` |
| **有效取值** | 正整數（秒） |
| **安全說明** | 超長的超時可能在重負載下耗盡工作者執行緒。較短的超時提高彈性，但可能導致對慢模型的不必要的重試。 |
| **常見錯誤** | 對於大履歷檔案或批量嵌入作業，將此值設得太低（< 10 秒）。 |

### `BACKEND_SERVICE_URL`

| 欄位 | 值 |
|------|-----|
| **用途** | AI 服務呼叫後端 API（例如向量 upsert）時使用的 URL。 |
| **預設值** | `http://backend:8080` |
| **有效取值** | AI 服務容器可達的任意 HTTP URL |
| **安全說明** | 使用 Docker 服務名稱 `backend`，使流量保持在內部網路中。 |
| **常見錯誤** | 在 AI 服務容器內部使用 `http://localhost:8080`。 |

### `BACKEND_QUERY_TIMEOUT`

| 欄位 | 值 |
|------|-----|
| **用途** | AI 服務呼叫後端 API 時的超時秒數。 |
| **預設值** | `5` |
| **有效取值** | 正整數（秒） |
| **安全說明** | 較短的超時可防止 AI 服務在 backend 過載時當機。 |
| **常見錯誤** | 批量向量 upsert 時將此值設得太低，導致部分資料插入。 |

---

## H. AI 服務日誌

### `LOG_LEVEL`

| 欄位 | 值 |
|------|-----|
| **用途** | 控制 AI 服務日誌記錄器（structlog）的詳細程度。 |
| **預設值** | `INFO` |
| **有效取值** | `DEBUG`、`INFO`、`WARNING`、`ERROR`、`CRITICAL` |
| **安全說明** | `DEBUG` 可能記錄包含履歷內容或使用者資料的請求負載。未經日誌脫敏和保留策略，切勿在生產環境中使用 `DEBUG`。 |
| **常見錯誤** | 生產環境中保留 `DEBUG` 等級，意外將個人識別資訊（PII）記錄到持續性日誌檔案中。 |

---

## I. Vertex AI 設定

### `VERTEX_PROJECT_ID`

| 欄位 | 值 |
|------|-----|
| **用途** | Vertex AI 的 Google Cloud 專案 ID（僅在使用 `vertex_ai/` 模型前綴時需要）。 |
| **預設值** | `ser594-ai-service` |
| **有效取值** | 任意有效的 Google Cloud 專案 ID |
| **安全說明** | 不是機密。僅用於識別計費專案。 |
| **常見錯誤** | 透過 AI Studio 使用 Gemini（`gemini/` 前綴）時設定此值——該模式下此值被忽略。 |

### `VERTEX_LOCATION`

| 欄位 | 值 |
|------|-----|
| **用途** | Vertex AI 模型推理的區域/位置。 |
| **預設值** | `global` |
| **有效取值** | `global`、`us-central1`、`europe-west4`、`asia-northeast1` 等 |
| **安全說明** | 選擇靠近部署位置的區域以最小化延遲，並滿足資料駐留合規要求。 |
| **常見錯誤** | 使用所選模型不可用的區域，導致 `404 Model not found` 錯誤。 |

### `VERTEX_CREDENTIALS`

| 欄位 | 值 |
|------|-----|
| **用途** | Google Cloud 服務帳號 JSON 金鑰檔案的主機絕對路徑。以唯讀卷形式掛載到 AI 服務容器中。 |
| **預設值** | *（空）* |
| **有效取值** | 絕對檔案系統路徑，例如 `/home/user/service-account.json` |
| **安全說明** | **關鍵警告**：此值**必須**為絕對路徑。相對路徑或純檔案名稱（例如 `vertex.json`）會被 Docker/Podman 解釋為命名卷參考，從而靜默建立一個空卷而非掛載您的憑證檔案。該檔案以唯讀方式掛載（`:ro`）以防止意外修改。請將此金鑰儲存在金鑰管理器中並定期輪換。 |
| **常見錯誤** | <ul><li>使用相對路徑，如 `./vertex.json` 或 `~/keys/gcp.json`。</li><li>使用專案目錄內已被 `.gitignore` 的路徑，但忘記將檔案複製到部署主機。</li><li>透過 AI Studio 使用 Gemini（`gemini/` 前綴）時設定此值——AI Studio 模式不需要 ADC 憑證。</li></ul> |

---

## J. 內部 API 金鑰

### `INTERNAL_API_KEY`

| 欄位 | 值 |
|------|-----|
| **用途** | 後端與 AI 服務 REST 端點之間的共用金鑰。保護嵌入生成端點免受未經授權的內網存取。 |
| **預設值** | *（空）* |
| **有效取值** | 任意字串；建議長度 ≥ 32 個字元 |
| **安全說明** | 這是**應用層縱深防禦**金鑰。即使攻擊者攻破某個容器並獲得內部 Docker 網路的存取權限，沒有此金鑰也無法呼叫 LLM 嵌入端點。後端透過 `InternalApiKeyInterceptor` 自動將其附加到每個出站 REST 請求。AI 服務中介軟體在請求標頭缺失或不正確時返回 HTTP 401。 |
| **常見錯誤** | <ul><li>在 `docker-compose.yml` 中為 `backend` 和 `ai-service` 設定不同的值——兩個服務必須共用**完全相同的值**。</li><li>使用短且易被猜中的字串。</li><li>生產環境中忘記設定，因為它在為空時也能正常運作（兩端均跳過驗證）。</li></ul> |

**推薦產生方式：**

```bash
# 32 位元組 = 44 個 base64 字元
openssl rand -base64 32
```

若留空，後端攔截器和 AI 服務中介軟體均跳過檢查。這便於本地開發，但**絕不可**用於生產環境。
