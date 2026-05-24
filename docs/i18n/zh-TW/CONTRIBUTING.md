# 為 JobCopilot ResumeAssistant 做出貢獻

首先，感謝您考慮為 JobCopilot 做出貢獻！本專案基於六邊形架構（端口與適配器），是一個 AI 驅動的履歷與職位匹配助手。我們正在從學習專案轉型為開源就緒產品，您的貢獻至關重要。

## 目錄

- [快速開始](#快速開始)
- [開發環境](#開發環境)
- [分支策略](#分支策略)
- [提交規範](#提交規範)
- [程式碼風格](#程式碼風格)
- [測試要求](#測試要求)
- [拉取請求流程](#拉取請求流程)
- [架構合規性](#架構合規性)
- [文件](#文件)
- [發布流程](#發布流程)
- [社群](#社群)

---

## 快速開始

1. 在 GitHub 上**Fork**本倉庫。
2. 在本地**Clone**您的 fork：
   ```bash
   git clone https://github.com/YOUR_USERNAME/ser594_Team6-ResumeAssistant.git
   cd ser594_Team6-ResumeAssistant
   ```
3. **設定 upstream** 遠端：
   ```bash
   git remote add upstream https://github.com/original-owner/ser594_Team6-ResumeAssistant.git
   ```
4. 按照我們的[分支命名規範](#分支策略)建立一個分支。

---

## 開發環境

### 前置條件

| 元件 | 所需版本 |
|-----------|----------------|
| Java | 21 (LTS) |
| Maven | 3.9+ |
| Node.js | 20+ (前端) |
| Python | 3.11+ (AI 服務) |
| Docker & Docker Compose | 最新穩定版 |
| PostgreSQL | 15+ (需 pgvector 擴充) |
| MinIO | 最新版 (物件儲存) |
| RabbitMQ | 3.12+ (訊息佇列) |

### 快速啟動

```bash
# 1. 啟動基礎設施服務
docker compose -f docker-compose.yml.example up -d postgres minio rabbitmq

# 2. 設定環境
cp .env.example .env
# 根據您的本地設定編輯 .env

# 3. 建置後端
cd backend && mvn clean install -DskipTests

# 4. 啟動後端服務
cd backend/trigger && mvn spring-boot:run

# 5. 啟動前端（另一個終端機）
cd frontend && npm install && npm run dev

# 6. 啟動 AI 服務（另一個終端機）
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --reload
```

---

## 分支策略

我們使用 **GitHub Flow** —— 簡單、輕量，針對持續交付最佳化。

```
main (受保護)
  ↑
feat/PROJ-123-user-authentication
  ↑
fix/PROJ-456-login-timeout
  ↑
hotfix/PROJ-789-payment-crash
```

### 分支命名規範

```
{type}/{ticket-id}-{short-description}
```

| 類型 | 用途 |
|------|---------|
| `feat` | 新功能 |
| `fix` | Bug 修復 |
| `hotfix` | 生產環境緊急修復 |
| `chore` | 維護、相依性更新 |
| `docs` | 僅文件變更 |
| `refactor` | 程式碼重構，無行為變更 |
| `test` | 測試新增或修復 |
| `perf` | 效能改進 |
| `ci` | CI/CD 設定 |
| `style` | 僅程式碼格式化 |

**規則：**
- 全部小寫，空格用連字號替代
- 類型前綴後最多 50 個字元
- 有 ticket/issue 編號時必須包含
- 合併後分支自動刪除

### 分支生命週期目標

| 分支類型 | 目標生命週期 | 最大生命週期 |
|-------------|----------------|------------------|
| Feature | 1-3 天 | 5 天 |
| Bug fix | <1 天 | 2 天 |
| Hotfix | <4 小時 | 1 天 |

**規則：** 如果分支超過最大生命週期，必須拆分為更小的 PR。

---

## 提交規範

我們強制執行 **Conventional Commits**。每條提交資訊必須遵循此格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 類型參考

| 類型 | 使用時機 | 範例 |
|------|-------------|---------|
| `feat` | 新功能 | `feat(api): add resume upload endpoint` |
| `fix` | Bug 修復 | `fix(db): resolve N+1 query in job listing` |
| `perf` | 效能改進 | `perf(vector): optimize cosine similarity calculation` |
| `refactor` | 無行為變更 | `refactor(job): extract ScreenshotValidator from ApplicationService` |
| `docs` | 文件 | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | 僅測試 | `test(auth): add SSO edge cases` |
| `chore` | 建置/工具 | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD 變更 | `ci: add JaCoCo coverage threshold check` |
| `style` | 僅格式化 | `style: apply Spotless formatting` |
| `revert` | 回退之前提交 | `revert: feat(api): add resume upload endpoint` |

### 範圍參考

| 範圍 | 區域 |
|-------|------|
| `api` | API 層（DTO、Facade） |
| `app` | 應用層（ApplicationServices） |
| `domain` | 領域層（實體、值物件、端口） |
| `infra` | 基礎設施層（適配器、設定） |
| `trigger` | 觸發層（控制器、事件監聽器） |
| `db` | 資料庫（遷移、Schema） |
| `deploy` | 部署設定 |
| `docs` | 文件 |
| `ci` | CI/CD 流水線 |
| `frontend` | 前端應用 |
| `ai` | AI 服務（Python） |

### 品質規則

1. **原子提交** —— 每次提交一個邏輯變更
2. **祈使語氣** —— "add feature" 而非 "added feature"
3. **主題 ≤ 72 字元** —— 必須能放入 `git log --oneline`
4. **正文每行 72 字元** —— 終端可讀
5. **引用 Issue** —— `Fixes #123` 或 `Refs PROJ-456`
6. **main 上不允許 WIP 提交** —— 先 squash 或互動式 rebase
7. **破壞性變更必須顯式聲明：**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   Migration: Update all API clients to include "Bearer " before token.
   ```

---

## 程式碼風格

### Java（後端）

- **格式化器：** Spotless Maven 外掛（Google Java Format）
- **執行：** `cd backend && mvn spotless:apply`
- **檢查：** `cd backend && mvn spotless:check`

### TypeScript（前端）

- **格式化器：** Prettier
- **Linter：** ESLint
- **執行：** `cd frontend && npm run lint:fix`

### Python（AI 服務）

- **格式化器：** Black
- **Linter：** Ruff
- **執行：** `cd ai-service && ruff check . && black .`

### 架構規則（強制）

我們的後端遵循 **六邊形架構（端口與適配器）**。違規將在程式碼審查中被拒絕。

| 層級 | 可依賴 | 禁止依賴 |
|-------|-------------|-------------------|
| `trigger` | `app`, `api` | `domain`, `infrastructure` |
| `app` | `domain`, `api` | `infrastructure`, `trigger` |
| `domain` | 僅 `types` | `app`, `infrastructure`, `trigger`, `api` |
| `infrastructure` | `domain`, `app`, `api` | — |
| `api` | 僅 `types` | `app`, `domain`, `infrastructure`, `trigger` |
| `types` | 無（純共享類型） | 任何層級 |

**關鍵約束：**
- ApplicationService 不得超過 **150 行** —— 拆分為 DomainService 或用例
- `app` 或 `domain` 中禁止出現 `RestTemplate` / `HttpClient` / `JdbcTemplate`
- `domain` 中禁止 Spring 註解（`@Component`, `@Service`, `@Autowired`）
- 外部 API 回應禁止使用 `Map<String, Object>` —— 使用嚴格 DTO
- 領域實體必須有**行為方法**，不能只有 getter/setter

我們使用 **ArchUnit** 測試自動執行這些規則。執行：
```bash
cd backend && mvn test -pl app -Dtest="*ArchitectureTest*"
```

---

## 測試要求

### 最低覆蓋率（CI 強制執行）

| 層級 | 最低覆蓋率 |
|-------|-----------------|
| `domain` | 80% |
| `app` | 70% |
| `infrastructure` | 50% |
| `trigger` | 60% |

### 所需測試類型

1. **單元測試** —— JUnit 5 + Mockito + AssertJ
   ```bash
   cd backend && mvn test
   ```

2. **架構測試** —— ArchUnit
   ```bash
   cd backend && mvn test -Dtest="*ArchitectureTest*"
   ```

3. **整合測試** —— Spring Boot Test + Testcontainers
   ```bash
   cd backend && mvn verify -Pintegration-test
   ```

### 編寫優質測試

- 測試**行為**，而非實作
- 測試名稱使用 **Given-When-Then** 結構：
  ```java
  @Test
  void shouldRejectScreenshotLargerThan5MB() { ... }
  ```
- Mock 外部相依性；用真實物件測試領域邏輯
- 整合測試必須清理資料庫狀態（`@Transactional` 或 `@Sql`）

---

## 拉取請求流程

### 建立 PR 之前

1. Rebase 到最新 `main`：
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. 執行完整檢查套件：
   ```bash
   cd backend && mvn clean verify
   cd frontend && npm run lint && npm run test
   cd ai-service && ruff check . && pytest
   ```
3. 確保分支符合命名規範
4. Squash WIP 提交：`git rebase -i upstream/main`

### PR 範本

開啟 PR 時範本會自動填充。填寫所有部分：

- **What** —— 一句話描述
- **Why** —— 連結到 Issue 或解釋問題
- **How** —— 技術方案和關鍵決策
- **Testing** —— 已執行測試清單
- **Screenshots** —— UI 變更時提供
- **Checklist** —— 自審清單

### PR 大小指南

| 大小 | 變更行數 | 預期審查時間 |
|------|--------------|---------------------|
| XS | <10 | 5 分鐘 |
| S | 10-50 | 15 分鐘 |
| M | 50-200 | 30 分鐘 |
| L | 200-500 | 60 分鐘 |

**規則：** >500 行的 PR 缺陷率高出 40%。積極拆分。

### 審查流程

| 變更類型 | 最少批准數 | 要求的審查者 |
|-------------|------------------|-------------------|
| Feature | 2 | 1 名領域專家 |
| Bug fix | 1 | 任何團隊成員 |
| Hotfix | 1 | On-call + 負責人 |
| Refactor | 2 | 原作者（如可用） |
| Docs only | 1 | 任何人 |
| Dependency update | 1 | 安全意識審查者 |
| Database migration | 2 | DBA/資深 + 1 名開發 |

### 審查評論分類

為評論加前綴以明確意圖：

| 前綴 | 含義 | 阻塞合併？ |
|--------|---------|--------------|
| `blocking:` | 合併前必須修復 | 是 |
| `suggestion:` | 考慮此改進 | 否 |
| `nit:` | 風格/格式偏好 | 否 |
| `question:` | 需要澄清 | 可能 |
| `praise:` | 做得好 | 否 |
| `thought:` | 長期考慮 | 否 |

---

## 架構合規性

### 六邊形架構檢查清單

提交 PR 前驗證：

- [ ] ApplicationServices 僅做編排，不包含業務邏輯
- [ ] 領域實體有行為方法（不只是 getter/setter）
- [ ] `domain` 或 `app` 中無框架程式碼（`RestTemplate`, `JPA`, `RabbitTemplate`）
- [ ] 所有外部相依性透過 Port 介面接入
- [ ] DTO 位於 `api` 層，絕不洩漏到 `domain`
- [ ] ValueObjects 不可變且在構造時驗證
- [ ] HTTP 呼叫或外部服務呼叫周圍無 `@Transactional`

### 常見違規避免

❌ **上帝 ApplicationService** (>150 行) → 提取 DomainService 或用例  
❌ 外部 API 回應使用 `Map<String, Object>` → 定義嚴格 DTO  
❌ `@Transactional` 圍繞 `RestTemplate` 呼叫 → 將 HTTP 呼叫移出事務  
❌ 控制器中包含業務邏輯 → 移到 ApplicationService 或 DomainService  
❌ 領域層使用 Spring 註解 → 使用純 Java + 在應用層使用建構子注入  
❌ 倉儲介面回傳框架類型 → 僅回傳領域類型  

---

## 文件

### 什麼需要文件

所有面向使用者或貢獻者的文件必須提供 **三種語言**：
- **英語**（主文件，根目錄）
- **簡體中文** (`docs/i18n/zh-CN/`)
- **繁體中文** (`docs/i18n/zh-TW/`)

| 文件 | 位置 | 需要 i18n? |
|----------|----------|---------------|
| README.md | `/` | 是 |
| CONTRIBUTING.md | `/` | 是 |
| CODE_OF_CONDUCT.md | `/` | 是 |
| CHANGELOG.md | `/` | 是 |
| 部署文件 | `docs/deployment/` | 是 |
| ADR | `docs/adr/` | 是 |
| API 文件 | 自動產生 (OpenAPI) | 否 |
| LICENSE | `/` | 否 |

### 架構決策記錄 (ADRs)

任何重大架構決策在 `docs/adr/` 中建立 ADR：

```
docs/adr/
├── 001-hexagonal-architecture.md
├── 002-postgresql-pgvector.md
├── 003-rabbitmq-message-queue.md
└── 004-minio-object-storage.md
```

ADR 範本：
```markdown
# ADR-XXX: 標題

## 狀態
Proposed / Accepted / Deprecated / Superseded

## 背景
我們要解決什麼問題？

## 決策
我們決定了什麼？

## 後果
正面和負面的結果。
```

---

## 發布流程

我們遵循 **語意化版本控制 (SemVer)**，使用 **release-please** 進行自動發布。

### 版本升級規則

| 變更類型 | 版本升級 | 範例 |
|-------------|-------------|---------|
| 破壞性 API 變更 | MAJOR | 刪除端點、變更回應結構 |
| 新功能（向後相容） | MINOR | 新增端點、新的選用欄位 |
| Bug 修復 | PATCH | 修復計算錯誤、拼字錯誤 |

### 自動發布流水線

1. 使用 conventional commits 合併 PR 到 `main`
2. `release-please` 建立帶有 changelog + 版本升級的發布 PR
3. 人工審查並合併發布 PR
4. 自動建立 Git 標籤 `vX.Y.Z`
5. 發布帶自動生成說明的 GitHub Release
6. 建置並推送 Docker 鏡像 `ghcr.io/jobcopilot/resumeassistant:vX.Y.Z`

### 手動發布（僅限緊急情況）

```bash
# 僅用於自動化故障時的熱修復
git tag -a v1.2.1 -m "Hotfix: resolve connection pool starvation"
git push upstream v1.2.1
```

---

## 社群

### 溝通管道

- **Issues：** Bug 報告、功能請求、問題
- **Discussions：** 架構提案、一般問題
- **Security：** 安全漏洞請郵件 **security@jobcopilot.dev**（請勿公開建立 Issue）

### 取得協助

1. 搜尋現有 Issues 和 Discussions
2. 查閱 `docs/` 中的文件
3. 在 Discussions 中使用 `question` 標籤提問
4. Bug 請使用 Bug Report Issue 範本

### 致謝

貢獻者將在以下位置獲得認可：
- 每次發布的 `CHANGELOG.md`
- `CONTRIBUTORS.md`（每季更新）
- GitHub 發布說明

---

## 致謝

本貢獻指南改編自：
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [Contributor Covenant](https://www.contributor-covenant.org/)

感謝您讓 JobCopilot 變得更好！🚀
