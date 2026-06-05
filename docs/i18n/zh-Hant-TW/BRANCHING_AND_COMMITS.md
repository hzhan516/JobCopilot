# 分支策略與提交規範

> **Languages:** [English](../../BRANCHING_AND_COMMITS.md) | [简体中文](../zh-Hans-CN/BRANCHING_AND_COMMITS.md) | 繁體中文 (current)

本文檔定義了 JobCopilot JobCopilot 專案的 Git 工程標準，涵蓋分支策略、提交規範、程式碼審查流程和發布管理。

> **狀態：** 已採納  
> **範圍：** JobCopilot 組織下的所有倉庫  
> **語言：** 英語（主文件） | [English](../../BRANCHING_AND_COMMITS.md) | [简体中文](../zh-Hans-CN/BRANCHING_AND_COMMITS.md)

---

## 1. 分支策略：GitHub Flow

我們使用 **GitHub Flow** —— 一種輕量化的基於主幹的模型，針對持續交付最佳化。

```
main（受保護，始終可部署）
  ↑
feat/RES-123-resume-upload
  ↑
fix/RES-456-login-timeout
  ↑
hotfix/RES-789-connection-pool
```

### 為什麼選擇 GitHub Flow？

| 因素 | 我們的情況 | 適配度 |
|--------|--------------|--------|
| 團隊規模 | 3 名核心成員 | ✅ 完美適配 |
| 發布節奏 | 每週至雙週 | ✅ 完美適配 |
| CI/CD 成熟度 | 正在建設中（CI就緒，CD進行中） | ✅ 良好 |
| 功能開關 | 尚未實現 | ⚠️ 可接受 |
| 複雜度 | 低 —— 簡單的 trunk + feature 分支 | ✅ 完美適配 |

### 被我們拒絕的替代策略

| 策略 | 未採用原因 |
|----------|-------------|
| **GitFlow** | 對我們的節奏來說太重；`develop` 分支增加開銷卻無實際價值 |
| **Trunk-Based** | 需要功能開關和 >80% 覆蓋率 —— 尚未就緒 |
| **Release Flow** | 團隊 < 10 人時過度複雜 |

### 分支命名規範

```
{type}/{ticket-id}-{short-description}
```

**類型：**

| 類型 | 用途 | 範例 |
|------|---------|---------|
| `feat` | 新功能 | `feat/RES-123-resume-upload` |
| `fix` | Bug 修復 | `fix/RES-456-login-timeout` |
| `hotfix` | 生產環境緊急修復 | `hotfix/RES-789-connection-pool` |
| `chore` | 維護、相依性更新 | `chore/RES-200-bump-spring-boot` |
| `docs` | 僅文件變更 | `docs/RES-300-deployment-guide` |
| `refactor` | 程式碼重構，無行為變更 | `refactor/RES-400-extract-validator` |
| `test` | 測試新增 | `test/RES-500-auth-edge-cases` |
| `perf` | 效能改進 | `perf/RES-600-vector-caching` |
| `ci` | CI/CD 設定 | `ci/RES-700-add-jacoco` |
| `style` | 程式碼格式化 | `style/RES-800-spotless-format` |

**規則：**
- 全部小寫，空格用連字號替代
- `type/` 後最多 50 個字元
- 有 ticket/issue 編號時必須包含
- 合併後分支自動刪除（通過 GitHub 設定）

### 分支生命週期目標

| 分支類型 | 目標生命週期 | 最大生命週期 | 超期處理 |
|-------------|----------------|------------------|-------------------|
| Feature | 1-3 天 | 5 天 | 必須拆分為更小的 PR |
| Bug fix | <1 天 | 2 天 | 優先審查 |
| Hotfix | <4 小時 | 1 天 | 緊急審查流程 |

**>500 行的 PR 缺陷率高出 40%。積極拆分。**

---

## 2. 提交規範：Conventional Commits

每次提交必須遵循 [Conventional Commits](https://www.conventionalcommits.org/) 規範。

### 格式

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
| `perf` | 效能改進 | `perf(vector): optimize cosine similarity` |
| `refactor` | 無行為變更 | `refactor(job): extract ScreenshotValidator` |
| `docs` | 文件 | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | 僅測試 | `test(auth): add SSO edge cases` |
| `chore` | 建置/工具 | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD 變更 | `ci: add JaCoCo coverage threshold` |
| `style` | 僅格式化 | `style: apply Spotless formatting` |
| `revert` | 回退之前提交 | `revert: feat(api): add resume upload` |

### 範圍參考

| 範圍 | 區域 | 典型檔案 |
|-------|------|--------------|
| `api` | API 層 | `backend/api/src/...` |
| `app` | 應用層 | `backend/app/src/...` |
| `domain` | 領域層 | `backend/domain/src/...` |
| `infra` | 基礎設施層 | `backend/infrastructure/src/...` |
| `trigger` | 觸發層 | `backend/trigger/src/...` |
| `db` | 資料庫 | `backend/**/db/`, migrations |
| `deploy` | 部署 | `docker-compose*.yml`, `.env.example` |
| `docs` | 文件 | `docs/`, `README.md` |
| `ci` | CI/CD 流水線 | `.github/workflows/` |
| `frontend` | 前端應用 | `frontend/` |
| `ai` | AI 服務 | `ai-service/` |

### 品質規則

1. **原子提交** —— 每次提交一個邏輯變更
2. **祈使語氣** —— "add feature" 而非 "added feature"
3. **主題 ≤ 72 字元** —— 必須能放入 `git log --oneline`
4. **正文每行 72 字元** —— 終端可讀
5. **引用 Issue** —— `Fixes #123` 或 `Refs RES-456`
6. **main 上不允許 WIP 提交** —— 先 squash 或互動式 rebase
7. **破壞性變更必須顯式聲明：**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   ```

---

## 3. 程式碼審查流程

### PR 範本

所有 PR 使用 `.github/PULL_REQUEST_TEMPLATE.md` 中的範本。必需部分：

- **What** —— 一句話
- **Why** —— 連結到 Issue 或問題陳述
- **How** —— 技術方案、關鍵決策
- **Testing** —— 帶強制檢查的檢查清單
- **Checklist** —— 自審查驗證

### PR 大小指南

| 大小 | 變更行數 | 審查時間 | 缺陷率 |
|------|--------------|-------------|-------------|
| XS | <10 | 5 分鐘 | ~0% |
| S | 10-50 | 15 分鐘 | ~5% |
| M | 50-200 | 30 分鐘 | ~15% |
| L | 200-500 | 60 分鐘 | ~25% |
| XL | >500 | 120+ 分鐘 | ~40% |

**規則：PR >400 行必須拆分。**

### 審查 SLA

| 優先級 | 首次審查 | 批准 | 升級 |
|----------|-------------|----------|------------|
| Hotfix | 30 分鐘 | 1 小時 | 通知 on-call |
| Critical | 2 小時 | 4 小時 | DM 團隊負責人 |
| Normal | 4 小時 | 24 小時 | 每日站會 |
| Low | 24 小時 | 48 小時 | 每週審查 |

### 審查評論分類

所有審查評論加前綴：

| 前綴 | 含義 | 阻塞合併？ |
|--------|---------|--------------|
| `blocking:` | 合併前必須修復 | 是 |
| `suggestion:` | 考慮此改進 | 否 |
| `nit:` | 風格/格式偏好 | 否 |
| `question:` | 需要澄清 | 可能 |
| `praise:` | 做得好 | 否 |
| `thought:` | 長期考慮 | 否 |

### 批准規則

| 變更類型 | 最少批准數 | 要求審查者 | 自動合併？ |
|-------------|--------------|-------------------|------------|
| Feature | 2 | 1 名領域專家 | 否 |
| Bug fix | 1 | 任何團隊成員 | 可選 |
| Hotfix | 1 | On-call + 負責人 | 部署後 |
| Refactor | 2 | 原作者（如可用） | 否 |
| Docs only | 1 | 任何人 | 是 |
| Dependency update | 1 | 安全意識審查者 | Dependabot: 是 |
| Config change | 2 | Ops + dev | 否 |
| Database migration | 2 | DBA/資深 + 1 名開發 | 否 |

---

## 4. 分支保護規則

### `main` 分支

```yaml
required_reviews: 2
dismiss_stale_reviews: true
require_code_owner_reviews: true
require_signed_commits: false  # 團隊就緒後啟用
require_linear_history: true    # 無合併提交
require_status_checks:
  - "Backend Build & Test"
  - "Frontend Build & Test"
  - "AI Service Build & Test"
  - "Security Scan"
  - "Docker Build Test"
restrict_push: [release-bot]
allow_force_push: false
allow_deletions: false
require_conversation_resolution: true
```

### `develop` 分支（如使用）

```yaml
required_reviews: 1
require_status_checks:
  - "Backend Build & Test"
  - "Frontend Build & Test"
```

---

## 5. CI/CD 整合

### 合併前 CI 流水線

CI 在每個 PR 和 push 到 `main`/`develop` 時執行：

| 階段 | 檢查 | 目標時長 |
|-------|--------|----------------|
| 格式化與風格 | Spotless, ESLint, Ruff, Prettier | <30秒 |
| 型別檢查 | TypeScript strict, mypy | <60秒 |
| 單元測試 | JUnit 5, pytest | <3 分鐘 |
| 整合測試 | Spring Boot + Testcontainers | <5 分鐘 |
| 安全掃描 | Trivy, OWASP dependency-check | <2 分鐘 |
| 建置 | Maven package, Vite build, Docker build | <3 分鐘 |

**總目標：<10 分鐘**

### CI 規則

- 所有檢查通過才能合併
- 不穩定測試 24 小時內隔離
- 新程式碼不得降低覆蓋率
- 安全發現（HIGH/CRITICAL）阻塞合併

---

## 6. 發布管理

### 語意化版本控制 (SemVer)

```
MAJOR.MINOR.PATCH[-prerelease]

範例：
  1.0.0        → 首個穩定版本
  1.1.0        → 新功能，向後相容
  1.1.1        → Bug 修復
  2.0.0        → 破壞性變更
```

### 版本升級決策矩陣

| 變更類型 | 版本升級 | 範例 |
|-------------|-------------|---------|
| 破壞性 API 變更 | MAJOR | 刪除端點、變更回應結構 |
| 新功能（向後相容） | MINOR | 新增端點、新的選用欄位 |
| Bug 修復 | PATCH | 修復計算錯誤、拼字錯誤 |
| 效能改進 | PATCH | 最佳化查詢（行為不變） |
| 相依性更新（相容） | PATCH | 升級 lodash minor |
| 相依性更新（破壞性） | MAJOR or MINOR | 評估下游影響 |

### 容器優先發布

我們**只發布 Docker 鏡像** —— 不發布到 Maven Central、npm registry 或 PyPI。

**製品倉庫：** `ghcr.io/jobcopilot/resumeassistant`

| 組件 | 鏡像標籤 | 範例 |
|-----------|-----------|---------|
| 後端 | `ghcr.io/jobcopilot/resumeassistant/backend:vX.Y.Z` | `backend:v1.2.0` |
| 前端 | `ghcr.io/jobcopilot/resumeassistant/frontend:vX.Y.Z` | `frontend:v1.2.0` |
| AI 服務 | `ghcr.io/jobcopilot/resumeassistant/ai-service:vX.Y.Z` | `ai-service:v1.2.0` |

**標籤策略：**
- `vX.Y.Z` —— 精確發布（不可變）
- `vX.Y` —— minor 線（最新 patch）
- `vX` —— major 線（最新 minor）
- `latest` —— 滾動更新，僅用於開發/測試

### 自動發布 (release-please)

我們使用 [release-please](https://github.com/googleapis/release-please) 進行零接觸發布：

1. 使用 conventional commits 合併 PR 到 `main`
2. `release-please` 建立發布 PR，包含：
   - `package.json` / `pom.xml` 版本升級
   - `CHANGELOG.md` 更新
3. 人工審查並合併發布 PR
4. 自動建立 Git 標籤 `vX.Y.Z`
5. 發布帶自動生成說明的 GitHub Release
6. 建置並推送 Docker 鏡像到 `ghcr.io/jobcopilot/resumeassistant/*:vX.Y.Z`
7. **不發布到套件管理器** —— 容器是唯一的分發製品

### 熱修復流程

```
1. 從最新發布標籤建立分支：
   git checkout -b hotfix/RES-XXX-description v1.2.0

2. 實現修復並附帶測試

3. 帶 'hotfix' 標籤的 PR → 加速審查（1 名審查者）

4. 合併到 main 並 cherry-pick 到發布分支（如存在）

5. 立即打補丁標籤：
   git tag -a v1.2.1 -m "Hotfix: resolve ..."
   
   # 容器鏡像通過 GitHub Actions 從標籤自動重建

6. 部署到生產環境

7. 事後：將迴歸測試新增到 CI
```

**SLA：發現後 4 小時內完成修復部署**

---

## 7. Git 安全

### 憑證洩漏預防

**Pre-commit hooks**（通過 lefthook 或 husky 管理）：
- 對暫存檔案執行 gitleaks 掃描
- 檢查 detect-secrets 基線

**CI 掃描：**
- 每個 PR 執行 Trivy 檔案系統掃描
- CI 流水線中執行 truffleHog

### 緊急響應（憑證暴露）

1. **立即撤銷憑證** —— 不要等待歷史清理
2. 使用 `git filter-repo` 或 BFG Repo-Cleaner 移除
3. 強制推送清理後的歷史
4. 聯絡 GitHub 支援清除快取
5. 審計憑證使用日誌
6. 輪換所有可能暴露的憑證
7. 將模式新增到 pre-commit hooks

> **⚠️ 警告：** 即使從歷史中移除，也假設憑證已洩露。複製過倉庫的人可能在快取中保留了它。

### 提交簽名（推薦）

```bash
# SSH 簽名（比 GPG 更簡單）
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

---

## 8. 倉庫衛生

### .gitignore 檢查清單

**始終忽略：**
- `node_modules/`, `venv/`, `__pycache__/`
- `.env`, `.env.local`, `.env.*.local`
- `*.key`, `*.pem`, `*.p12`
- `.DS_Store`, `Thumbs.db`
- `*.log`, `logs/`
- `dist/`, `build/`, `out/`
- `coverage/`, `.nyc_output/`
- `.idea/`, `.vscode/`（共享設定除外）

**永不忽略：**
- `.gitignore` 本身
- Lockfiles（`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`）
- `.env.example`（無秘密的範本）
- `docker-compose.yml`
- `Makefile`, `Taskfile`

### 過期分支清理

- 合併後分支自動刪除（GitHub 設定）
- 每月審查過期分支（>30 天）
- `git branch -r --merged main | grep -v main | xargs -n1 git push --delete origin`

---

## 9. 指標與健康看板

### 每週追蹤

| 指標 | 良好 | 優秀 | 目標 |
|--------|------|-------|--------|
| PR 審查時間 | <24h | <4h | <4h |
| PR 合併時間 | <48h | <24h | <24h |
| CI 流水線 | <15 分鐘 | <10 分鐘 | <10 分鐘 |
| CI 通過率 | >90% | >95% | >95% |
| 分支生命週期 | <5 天 | <3 天 | <3 天 |
| 過期分支 | <20 | <10 | 0 |
| 程式碼審查覆蓋率 | >80% | >95% | 100% |

### 倉庫健康評分

每週在 `.github/health-dashboard.md` 追蹤

---

## 10. 常用 Git 命令速查表

```bash
# 開始功能開發
git checkout -b feat/RES-123-description main

# 保持分支 rebase
git fetch upstream
git rebase upstream/main

# PR 前清理
git rebase -i upstream/main
# pick → 保留
# squash → 與前一個合併
# fixup → 合併，丟棄資訊
# reword → 修改資訊
# drop → 刪除

# 修復最後一條提交資訊
git commit --amend

# 撤銷最後提交（保留變更）
git reset --soft HEAD~1

# 尋找丟失的提交
git reflog

# 恢復刪除的分支
git reflog | grep "checkout: moving"
git checkout -b recovered-branch <sha>

# 從歷史中移除檔案 (BFG)
bfg --delete-files filename
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

---

## 致謝

本文檔改編自：
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [AfrexAI Git Engineering](https://github.com/afrexai/git-engineering)

---

*最後更新：2026-05-25*