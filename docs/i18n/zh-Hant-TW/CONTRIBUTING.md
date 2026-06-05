# 貢獻指南

> **語言**: [English](../../../CONTRIBUTING.md) | [簡體中文](../zh-Hans-CN/CONTRIBUTING.md) | **繁體中文**

感謝您對 JobCopilot 的興趣！本文件提供指南和說明，幫助您快速上手。

---

## 目錄

- [行為準則](#行為準則)
- [快速開始](#快速開始)
- [開發環境搭建](#開發環境搭建)
- [專案結構](#專案結構)
- [分支策略](#分支策略)
- [提交規範](#提交規範)
- [Pull Request 流程](#pull-request-流程)
- [架構指南](#架構指南)
- [測試要求](#測試要求)
- [程式碼風格](#程式碼風格)
- [文件](#文件)
- [發布流程](#發布流程)
- [社群](#社群)

---

## 行為準則

本專案遵循尊重、建設性協作的標準。參與即表示您同意：

- 在所有互動中保持尊重和包容
- 優雅地接受建設性批評
- 關注對社群和專案最有益的事項
- 對其他社群成員展現同理心

---

## 快速開始

### 前置條件

| 元件 | 最低版本 | 推薦版本 |
|-----------|----------------|-------------|
| Java      | 21             | 21 (LTS)    |
| Maven     | 3.9.6          | 3.9.9       |
| Node.js   | 20.11.0        | 20.x LTS    |
| Python    | 3.11           | 3.11+       |
| Docker    | 24.0.0         | 27.x        |
| Docker Compose | 2.20.0    | 2.29+       |

> 驗證環境：`java -version`、`mvn -v`、`node -v`、`python3 --version`、`docker --version`

### Fork 與克隆

```bash
# 在 GitHub 上 Fork 儲存庫，然後克隆您的 Fork
git clone <your-fork-url>
cd <repository-name>

# 添加上游遠端儲存庫
git remote add upstream <upstream-repository-url>
```

---

## 開發環境搭建

### 方案 A：Docker Compose（推薦首次貢獻者使用）

```bash
cp .env.example .env
# 編輯 .env 配置您的參數
docker compose --env-file .env up -d --build
```

### 方案 B：本地開發

```bash
# 1. 啟動基礎設施服務
cp .env.example .env
# 編輯 .env 配置您的本地參數
docker compose --env-file .env up -d postgres redis rabbitmq minio

# 2. 後端
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl app

# 3. 前端（新終端機）
cd frontend
npm install
npm run dev

# 4. AI 服務（新終端機，選用）
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### 環境變數

將 `.env.example` 複製為 `.env` 並配置：
- 資料庫憑證（PostgreSQL + pgvector）
- MinIO / S3 相容儲存
- RabbitMQ 連線
- LiteLLM 提供商 API 金鑰，例如 Gemini、OpenAI 或 Anthropic
- Google OAuth 憑證

詳見 `docs/deployment/` 詳細配置參考。

---

## 專案結構

```
.
|-- backend/           # Java / Spring Boot 後端，採用 DDD 與六邊形模組
|   |-- api/           # DTO、命令、查詢和外觀介面
|   |-- app/           # 應用服務、排程器和啟動裝配
|   |-- domain/        # 實體、值物件、連接埠和領域規則
|   |-- infrastructure/ # 持久化、儲存、訊息、安全和外部整合
|   |-- trigger/       # REST 控制器、WebSocket 端點、MQ 監聽器
|   `-- types/         # 共享型別與常數
|-- frontend/          # React / Vite / TypeScript 應用
|   `-- src/           # 元件、頁面、服務、狀態、Hooks、i18n、工具
|-- ai-service/        # Python / FastAPI AI 服務和 Worker
|   |-- app/           # API、領域、基礎設施、MQ、服務和 Worker 程式碼
|   `-- tests/         # Pytest 測試套件
|-- docs/              # ADR、API 文件、架構、部署和國際化
|-- eval/              # AI 評估腳本、資料集和結果
|-- middleware/        # 自訂基礎設施映像，例如 PostgreSQL
|-- scripts/           # 倉庫級自動化輔助腳本
|-- .github/           # CI、Issue 模板、PR 模板、CODEOWNERS
|-- docker-compose.yml # 本地 Docker Compose 堆疊
`-- .env.example       # 環境變數模板
```

### 架構原則

- **領域驅動設計（DDD）**：業務邏輯位於 `domain/`；無框架依賴
- **六邊形架構（Ports & Adapters）**：領域定義連接埠；基礎設施實作介面卡
- **Outbox 模式**：所有非同步訊息透過資料庫 outbox 表確保可靠性
- **CQRS**：在適用場景分離命令與查詢職責

---

## 分支策略

我們採用適合開源協作的輕量主幹開發流程：

| 分支 | 用途 | 保護規則 |
|--------|---------|------------|
| `main` | 穩定開發分支和發布來源 | 受保護；需要 PR + 審查 |
| `feature/*` | 新功能 | 短生命週期分支；透過 PR 合併到 `main` |
| `fix/*` | Bug 修復 | 短生命週期分支；透過 PR 合併到 `main` |
| `docs/*` | 僅文件變更 | 短生命週期分支；透過 PR 合併到 `main` |
| `chore/*` | 維護、依賴、工具鏈變更 | 短生命週期分支；透過 PR 合併到 `main` |
| `release/v*` | 可選的發布穩定分支 | 僅在維護者準備發布時建立 |

### 工作流程

```bash
# 開始新功能
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name

# 工作、提交、推送
git add .
git commit -m "feat(matching): add vector caching for recall"
git push origin feature/your-feature-name

# 針對 main 開啟 Pull Request
```

---

## 提交規範

我們使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hant/) 規範：

| 類型 | 描述 | 示例 |
|------|-------------|---------|
| `feat` | 新功能 | `feat(auth): add Google OAuth login` |
| `fix` | Bug 修復 | `fix(tx): resolve nested transaction in matching` |
| `docs` | 僅文件 | `docs(deploy): add Kubernetes deployment guide` |
| `style` | 程式碼風格（格式化） | `style(frontend): apply prettier to components` |
| `refactor` | 重構 | `refactor(domain): extract JobValidator from service` |
| `perf` | 效能最佳化 | `perf(embedding): batch vector generation` |
| `test` | 新增或修正測試 | `test(matching): add MatchTransactionService tests` |
| `chore` | 維護 / 依賴 | `chore(deps): bump Spring Boot to 3.5.7` |
| `ci` | CI/CD 變更 | `ci: add OWASP dependency check` |
| `build` | 建置系統變更 | `build: configure maven-enforcer-plugin` |
| `revert` | 回滾提交 | `revert: rollback vector cache (regression)` |

### 作用域規範

使用元件級作用域：`auth`、`job`、`resume`、`matching`、`embedding`、`conversation`、`tracking`、`domain`、`infrastructure`、`frontend`、`ai`、`deploy`、`docs`。

### 破壞性變更

在描述前加 `!` 或新增 `BREAKING CHANGE:` 註腳：

```
feat(api)!: remove deprecated v1 endpoints

BREAKING CHANGE: /api/v1/* 端點已移除。請遷移至 /api/v2/*。
```

---

## Pull Request 流程

### 提交前

1. **更新分支**：`git pull upstream main`
2. **本地執行品質檢查**：
   ```bash
   # 後端
   cd backend && mvn clean verify

   # 前端
   cd frontend && npm run lint && npm run test:run

   # AI 服務
   cd ai-service && ruff check . && pytest
   ```
3. **為新邏輯撰寫或更新測試**
4. **如果使用戶可見行為變更，更新文件**
5. **審查您的 diff** — 保持變更聚焦且最小化

### PR 模板

PR 必須包含：

- **What**：變更的清晰描述
- **Why**：動機和上下文（關聯 issue：`Fixes #123`）
- **How**：關鍵實作決策
- **Testing**：如何驗證變更
- **Checklist**：確認以下所有項

### 審查標準

- [ ] 程式碼遵循 DDD 六邊形架構
- [ ] `domain/` 模組無框架依賴
- [ ] 新增/更新的測試覆蓋修改行 ≥60%
- [ ] 無 `@Transactional` 與網路 I/O（HTTP、MQ、檔案）混用
- [ ] ESLint / Prettier 通過（前端）
- [ ] Maven 建置含測試通過
- [ ] 如有適用，文件已更新
- [ ] 提交資訊遵循 Conventional Commits

### 審查流程

1. 作者針對 `main` 開啟 PR
2. CI 檢查必須通過（建置、測試、Lint、安全掃描）
3. 至少需要一個維護者審查
4. 使用 fixup 提交處理審查回饋
5. 維護者 squash 合併

---

## 架構指南

### DDD 分層規則

```
┌─────────────────────────────────────┐
│  trigger  (Controllers, WebSocket)   │  ◄── HTTP / WebSocket 入口
├─────────────────────────────────────┤
│  app      (Application Services)     │  ◄── 編排、交易邊界
├─────────────────────────────────────┤
│  domain   (Entities, Ports, VO)    │  ◄── 純業務邏輯
├─────────────────────────────────────┤
│  infra    (Adapters, Repositories) │  ◄── DB、MQ、HTTP、儲存
└─────────────────────────────────────┘
```

| 規則 | 執行方式 |
|------|-------------|
| `domain/` 零 Spring / Hibernate 匯入（`javax.validation` 除外） | ArchUnit 測試：`noClasses().that().resideInAPackage("..domain..")..should().dependOnClassesThat().resideInAPackage("org.springframework..")` |
| 領域介面（Ports）位於 `domain/**/port/` | 程式碼審查 |
| Application Services 持有 `@Transactional`，Domain Services 不持有 | 檢查清單 |
| 基礎設施介面卡實作領域連接埠 | 編譯期 |

### 交易安全

- **命令**：`@Transactional`（讀寫）
- **查詢**：`@Transactional(readOnly = true)`
- **交易內不做網路 I/O**：HTTP 呼叫、MQ 傳送、檔案上傳必須在 `@Transactional` 外
- **Outbox 模式**：所有非同步訊息先寫入 `outbox` 表；scheduler 中繼

詳見 `docs/transactional-strategy.md` 完整策略。

---

## 測試要求

### 後端（Java）

| 測試類型 | 工具 | 最低覆蓋率 |
|-----------|------|-----------------|
| 單元測試 | JUnit 5 + Mockito | 60% 指令 / 行，40% 分支 |
| 架構測試 | ArchUnit | 100%（必須通過） |
| 整合測試 | `@SpringBootTest` | 僅關鍵流程 |

```bash
# 執行所有後端測試含覆蓋率
cd backend && mvn clean verify

# 執行特定模組測試
cd backend/app && mvn test
```

### 前端（TypeScript）

| 測試類型 | 工具 | 要求 |
|-----------|------|-------------|
| 單元測試 | Vitest + React Testing Library | 所有含邏輯元件 |
| E2E 測試 | Playwright（規劃中） | 關鍵使用者旅程 |

```bash
cd frontend
npm run test:run        # 一次性執行
npm run test:coverage   # 含覆蓋率報告
```

### AI 服務（Python）

| 測試類型 | 工具 | 要求 |
|-----------|------|-------------|
| 單元測試 | pytest | 所有服務函數 |
| 程式碼檢查 | ruff | 必須通過 |

```bash
cd ai-service
pytest --cov=app --cov-report=term-missing
```

---

## 程式碼風格

### Java

- **格式化器**：使用 Spring Boot 預設風格（4 空格，120 字元行寬）
- **Lombok**：允許在 `app/` 和 `infra/`；**不允許**在 `domain/`
- **匯入**：禁止萬用字元匯入；`Assertions`、`Mockito` 使用靜態匯入
- **空安全**：使用 `Optional<>`；避免從領域方法回傳 `null`

### TypeScript / React

- **格式化器**：Prettier（配置在 `frontend/prettier.config.js`）
- **程式碼檢查器**：ESLint with TypeScript、React Hooks、React Refresh 規則
- **元件**：函數元件 + Hooks；不使用類別元件
- **樣式**：TailwindCSS + `class-variance-authority` 做元件變體

```bash
# 自動修復前端問題
cd frontend
npm run lint            # 檢查
npm run format          # 格式化
```

### Python

- **格式化器**：Black 相容（透過 `ruff format`）
- **程式碼檢查器**：ruff 含 isort、flake8、pydocstyle 規則
- **類型註解**：所有函數簽名必須含類型註解

```bash
cd ai-service
ruff check .            # 檢查
ruff format .           # 格式化
```

---

## 文件

- **使用者可見變更**：更新 `docs/` 英文 + 中文（簡體 + 繁體）版本
- **架構決策**：新增條目到 `docs/adr/`（Architecture Decision Records）
- **API 變更**：OpenAPI 註解自動更新；透過 `mvn spring-boot:run` + `/swagger-ui.html` 驗證
- **部署變更**：更新 `docs/deployment/` 和 `.env.example`

---

## 發布流程

1. **版本號更新**：更新 `backend/pom.xml` 和 `frontend/package.json` 的 `version`
2. **更新日誌**：如果專案維護 `CHANGELOG.md`，用 Conventional Commits 摘要更新它
3. **打標籤**：`git tag -a v1.x.x -m "Release v1.x.x"`
4. **建置**：CI 建置 Docker 映像並推送至倉庫
5. **部署**：用新映像更新生產環境

---

## 社群

- **Issues**：使用 GitHub Issues 提交 Bug 報告和功能請求
- **Discussions**：使用 GitHub Discussions 提問和交流想法
- **安全漏洞**：私信維護者報告安全漏洞

---

## 有疑問？

如有任何不清楚的地方，請開啟 [GitHub Discussion](https://github.com/jobcopilot/jobcopilot/discussions) 或在 Issue 中提問。我們很樂意幫助。

**感謝貢獻！🚀**
