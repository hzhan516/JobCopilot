# 分支策略与提交规范

> **Languages:** [English](../../BRANCHING_AND_COMMITS.md) | 简体中文 (current) | [繁體中文](../zh-TW/BRANCHING_AND_COMMITS.md)

本文档定义了 JobCopilot ResumeAssistant 项目的 Git 工程标准，涵盖分支策略、提交规范、代码审查流程和发布管理。

> **状态：** 已采纳  
> **范围：** JobCopilot 组织下的所有仓库  
> **语言：** 英语（主文件） | [English](../../BRANCHING_AND_COMMITS.md) | [繁體中文](../zh-TW/BRANCHING_AND_COMMITS.md)

---

## 1. 分支策略：GitHub Flow

我们使用 **GitHub Flow** —— 一种轻量化的基于主干的模型，针对持续交付优化。

```
main（受保护，始终可部署）
  ↑
feat/RES-123-resume-upload
  ↑
fix/RES-456-login-timeout
  ↑
hotfix/RES-789-connection-pool
```

### 为什么选择 GitHub Flow？

| 因素 | 我们的情况 | 适配度 |
|--------|--------------|--------|
| 团队规模 | 3 名核心成员 | ✅ 完美适配 |
| 发布节奏 | 每周至双周 | ✅ 完美适配 |
| CI/CD 成熟度 | 正在建设中（CI已就绪，CD进行中） | ✅ 良好 |
| 功能开关 | 尚未实现 | ⚠️ 可接受 |
| 复杂度 | 低 —— 简单的 trunk + feature 分支 | ✅ 完美适配 |

### 被我们拒绝的替代策略

| 策略 | 未采用原因 |
|----------|-------------|
| **GitFlow** | 对我们的节奏来说太重；`develop` 分支增加开销却无实际价值 |
| **Trunk-Based** | 需要功能开关和 >80% 覆盖率 —— 尚未就绪 |
| **Release Flow** | 团队 < 10 人时过度复杂 |

### 分支命名规范

```
{type}/{ticket-id}-{short-description}
```

**类型：**

| 类型 | 用途 | 示例 |
|------|---------|---------|
| `feat` | 新功能 | `feat/RES-123-resume-upload` |
| `fix` | Bug 修复 | `fix/RES-456-login-timeout` |
| `hotfix` | 生产环境紧急修复 | `hotfix/RES-789-connection-pool` |
| `chore` | 维护、依赖更新 | `chore/RES-200-bump-spring-boot` |
| `docs` | 仅文档变更 | `docs/RES-300-deployment-guide` |
| `refactor` | 代码重构，无行为变更 | `refactor/RES-400-extract-validator` |
| `test` | 测试添加 | `test/RES-500-auth-edge-cases` |
| `perf` | 性能改进 | `perf/RES-600-vector-caching` |
| `ci` | CI/CD 配置 | `ci/RES-700-add-jacoco` |
| `style` | 代码格式化 | `style/RES-800-spotless-format` |

**规则：**
- 全部小写，空格用连字符替代
- `type/` 后最多 50 个字符
- 有 ticket/issue 编号时必须包含
- 合并后分支自动删除（通过 GitHub 设置）

### 分支生命周期目标

| 分支类型 | 目标生命周期 | 最大生命周期 | 超期处理 |
|-------------|----------------|------------------|-------------------|
| Feature | 1-3 天 | 5 天 | 必须拆分为更小的 PR |
| Bug fix | <1 天 | 2 天 | 优先审查 |
| Hotfix | <4 小时 | 1 天 | 紧急审查流程 |

**>500 行的 PR 缺陷率高出 40%。积极拆分。**

---

## 2. 提交规范：Conventional Commits

每次提交必须遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型参考

| 类型 | 使用时机 | 示例 |
|------|-------------|---------|
| `feat` | 新功能 | `feat(api): add resume upload endpoint` |
| `fix` | Bug 修复 | `fix(db): resolve N+1 query in job listing` |
| `perf` | 性能改进 | `perf(vector): optimize cosine similarity` |
| `refactor` | 无行为变更 | `refactor(job): extract ScreenshotValidator` |
| `docs` | 文档 | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | 仅测试 | `test(auth): add SSO edge cases` |
| `chore` | 构建/工具 | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD 变更 | `ci: add JaCoCo coverage threshold` |
| `style` | 仅格式化 | `style: apply Spotless formatting` |
| `revert` | 回退之前提交 | `revert: feat(api): add resume upload` |

### 范围参考

| 范围 | 区域 | 典型文件 |
|-------|------|--------------|
| `api` | API 层 | `backend/api/src/...` |
| `app` | 应用层 | `backend/app/src/...` |
| `domain` | 领域层 | `backend/domain/src/...` |
| `infra` | 基础设施层 | `backend/infrastructure/src/...` |
| `trigger` | 触发层 | `backend/trigger/src/...` |
| `db` | 数据库 | `backend/**/db/`, migrations |
| `deploy` | 部署 | `docker-compose*.yml`, `.env.example` |
| `docs` | 文档 | `docs/`, `README.md` |
| `ci` | CI/CD 流水线 | `.github/workflows/` |
| `frontend` | 前端应用 | `frontend/` |
| `ai` | AI 服务 | `ai-service/` |

### 质量规则

1. **原子提交** —— 每次提交一个逻辑变更
2. **祈使语气** —— "add feature" 而非 "added feature"
3. **主题 ≤ 72 字符** —— 必须能放入 `git log --oneline`
4. **正文每行 72 字符** —— 终端可读
5. **引用 Issue** —— `Fixes #123` 或 `Refs RES-456`
6. **main 上不允许 WIP 提交** —— 先 squash 或交互式 rebase
7. **破坏性变更必须显式声明：**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   ```

---

## 3. 代码审查流程

### PR 模板

所有 PR 使用 `.github/PULL_REQUEST_TEMPLATE.md` 中的模板。必需部分：

- **What** —— 一句话
- **Why** —— 链接到 Issue 或问题陈述
- **How** —— 技术方案、关键决策
- **Testing** —— 带强制检查的检查清单
- **Checklist** —— 自审查验证

### PR 大小指南

| 大小 | 变更行数 | 审查时间 | 缺陷率 |
|------|--------------|-------------|-------------|
| XS | <10 | 5 分钟 | ~0% |
| S | 10-50 | 15 分钟 | ~5% |
| M | 50-200 | 30 分钟 | ~15% |
| L | 200-500 | 60 分钟 | ~25% |
| XL | >500 | 120+ 分钟 | ~40% |

**规则：PR >400 行必须拆分。**

### 审查 SLA

| 优先级 | 首次审查 | 批准 | 升级 |
|----------|-------------|----------|------------|
| Hotfix | 30 分钟 | 1 小时 | 通知 on-call |
| Critical | 2 小时 | 4 小时 | DM 团队负责人 |
| Normal | 4 小时 | 24 小时 | 每日站会 |
| Low | 24 小时 | 48 小时 | 每周审查 |

### 审查评论分类

所有审查评论加前缀：

| 前缀 | 含义 | 阻塞合并？ |
|--------|---------|--------------|
| `blocking:` | 合并前必须修复 | 是 |
| `suggestion:` | 考虑此改进 | 否 |
| `nit:` | 风格/格式偏好 | 否 |
| `question:` | 需要澄清 | 可能 |
| `praise:` | 做得好 | 否 |
| `thought:` | 长期考虑 | 否 |

### 批准规则

| 变更类型 | 最少批准数 | 要求审查者 | 自动合并？ |
|-------------|--------------|-------------------|------------|
| Feature | 2 | 1 名领域专家 | 否 |
| Bug fix | 1 | 任何团队成员 | 可选 |
| Hotfix | 1 | On-call + 负责人 | 部署后 |
| Refactor | 2 | 原作者（如可用） | 否 |
| Docs only | 1 | 任何人 | 是 |
| Dependency update | 1 | 安全意识审查者 | Dependabot: 是 |
| Config change | 2 | Ops + dev | 否 |
| Database migration | 2 | DBA/资深 + 1 名开发 | 否 |

---

## 4. 分支保护规则

### `main` 分支

```yaml
required_reviews: 2
dismiss_stale_reviews: true
require_code_owner_reviews: true
require_signed_commits: false  # 团队就绪后启用
require_linear_history: true    # 无合并提交
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

## 5. CI/CD 集成

### 合并前 CI 流水线

CI 在每个 PR 和 push 到 `main`/`develop` 时运行：

| 阶段 | 检查 | 目标时长 |
|-------|--------|----------------|
| 格式化与风格 | Spotless, ESLint, Ruff, Prettier | <30秒 |
| 类型检查 | TypeScript strict, mypy | <60秒 |
| 单元测试 | JUnit 5, pytest | <3 分钟 |
| 集成测试 | Spring Boot + Testcontainers | <5 分钟 |
| 安全扫描 | Trivy, OWASP dependency-check | <2 分钟 |
| 构建 | Maven package, Vite build, Docker build | <3 分钟 |

**总目标：<10 分钟**

### CI 规则

- 所有检查通过才能合并
- 不稳定测试 24 小时内隔离
- 新代码不得降低覆盖率
- 安全发现（HIGH/CRITICAL）阻塞合并

---

## 6. 发布管理

### 语义化版本控制 (SemVer)

```
MAJOR.MINOR.PATCH[-prerelease]

示例：
  1.0.0        → 首个稳定版本
  1.1.0        → 新功能，向后兼容
  1.1.1        → Bug 修复
  2.0.0        → 破坏性变更
```

### 版本升级决策矩阵

| 变更类型 | 版本升级 | 示例 |
|-------------|-------------|---------|
| 破坏性 API 变更 | MAJOR | 删除端点、变更响应结构 |
| 新功能（向后兼容） | MINOR | 添加端点、新的可选字段 |
| Bug 修复 | PATCH | 修复计算错误、拼写错误 |
| 性能改进 | PATCH | 优化查询（行为不变） |
| 依赖更新（兼容） | PATCH | 升级 lodash minor |
| 依赖更新（破坏性） | MAJOR or MINOR | 评估下游影响 |

### 容器优先发布

我们**只发布 Docker 镜像** —— 不发布到 Maven Central、npm registry 或 PyPI。

**制品仓库：** `ghcr.io/jobcopilot/resumeassistant`

| 组件 | 镜像标签 | 示例 |
|-----------|-----------|---------|
| 后端 | `ghcr.io/jobcopilot/resumeassistant/backend:vX.Y.Z` | `backend:v1.2.0` |
| 前端 | `ghcr.io/jobcopilot/resumeassistant/frontend:vX.Y.Z` | `frontend:v1.2.0` |
| AI 服务 | `ghcr.io/jobcopilot/resumeassistant/ai-service:vX.Y.Z` | `ai-service:v1.2.0` |

**标签策略：**
- `vX.Y.Z` —— 精确发布（不可变）
- `vX.Y` —— minor 线（最新 patch）
- `vX` —— major 线（最新 minor）
- `latest` —— 滚动更新，仅用于开发/测试

### 自动发布 (release-please)

我们使用 [release-please](https://github.com/googleapis/release-please) 进行零接触发布：

1. 使用 conventional commits 合并 PR 到 `main`
2. `release-please` 创建发布 PR，包含：
   - `package.json` / `pom.xml` 版本升级
   - `CHANGELOG.md` 更新
3. 人工审查并合并发布 PR
4. 自动创建 Git 标签 `vX.Y.Z`
5. 发布带自动生成说明的 GitHub Release
6. 构建并推送 Docker 镜像到 `ghcr.io/jobcopilot/resumeassistant/*:vX.Y.Z`
7. **不发布到包管理器** —— 容器是唯一的分发制品

### 热修复流程

```
1. 从最新发布标签创建分支：
   git checkout -b hotfix/RES-XXX-description v1.2.0

2. 实现修复并附带测试

3. 带 'hotfix' 标签的 PR → 加速审查（1 名审查者）

4. 合并到 main 并 cherry-pick 到发布分支（如存在）

5. 立即打补丁标签：
   git tag -a v1.2.1 -m "Hotfix: resolve ..."
   
   # 容器镜像通过 GitHub Actions 从标签自动重建

6. 部署到生产环境

7. 事后：将回归测试添加到 CI
```

**SLA：发现后 4 小时内完成修复部署**

---

## 7. Git 安全

### 凭据泄漏预防

**Pre-commit hooks**（通过 lefthook 或 husky 管理）：
- 对暂存文件运行 gitleaks 扫描
- 检查 detect-secrets 基线

**CI 扫描：**
- 每个 PR 运行 Trivy 文件系统扫描
- CI 流水线中运行 truffleHog

### 紧急响应（凭据暴露）

1. **立即撤销凭据** —— 不要等待历史清理
2. 使用 `git filter-repo` 或 BFG Repo-Cleaner 移除
3. 强制推送清理后的历史
4. 联系 GitHub 支持清除缓存
5. 审计凭据使用日志
6. 轮换所有可能暴露的凭据
7. 将模式添加到 pre-commit hooks

> **⚠️ 警告：** 即使从历史中移除，也假设凭据已泄露。克隆过仓库的人可能在缓存中保留了它。

### 提交签名（推荐）

```bash
# SSH 签名（比 GPG 更简单）
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

---

## 8. 仓库卫生

### .gitignore 检查清单

**始终忽略：**
- `node_modules/`, `venv/`, `__pycache__/`
- `.env`, `.env.local`, `.env.*.local`
- `*.key`, `*.pem`, `*.p12`
- `.DS_Store`, `Thumbs.db`
- `*.log`, `logs/`
- `dist/`, `build/`, `out/`
- `coverage/`, `.nyc_output/`
- `.idea/`, `.vscode/`（共享设置除外）

**永不忽略：**
- `.gitignore` 本身
- Lockfiles（`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`）
- `.env.example`（无秘密的模板）
- `docker-compose.yml`
- `Makefile`, `Taskfile`

### 过期分支清理

- 合并后分支自动删除（GitHub 设置）
- 每月审查过期分支（>30 天）
- `git branch -r --merged main | grep -v main | xargs -n1 git push --delete origin`

---

## 9. 指标与健康看板

### 每周追踪

| 指标 | 良好 | 优秀 | 目标 |
|--------|------|-------|--------|
| PR 审查时间 | <24h | <4h | <4h |
| PR 合并时间 | <48h | <24h | <24h |
| CI 流水线 | <15 分钟 | <10 分钟 | <10 分钟 |
| CI 通过率 | >90% | >95% | >95% |
| 分支生命周期 | <5 天 | <3 天 | <3 天 |
| 过期分支 | <20 | <10 | 0 |
| 代码审查覆盖率 | >80% | >95% | 100% |

### 仓库健康评分

每周在 `.github/health-dashboard.md` 追踪

---

## 10. 常用 Git 命令速查表

```bash
# 开始功能开发
git checkout -b feat/RES-123-description main

# 保持分支 rebase
git fetch upstream
git rebase upstream/main

# PR 前清理
git rebase -i upstream/main
# pick → 保留
# squash → 与前一个合并
# fixup → 合并，丢弃信息
# reword → 修改信息
# drop → 删除

# 修复最后一条提交信息
git commit --amend

# 撤销最后提交（保留变更）
git reset --soft HEAD~1

# 查找丢失的提交
git reflog

# 恢复删除的分支
git reflog | grep "checkout: moving"
git checkout -b recovered-branch <sha>

# 从历史中移除文件 (BFG)
bfg --delete-files filename
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

---

## 致谢

本文档改编自：
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [AfrexAI Git Engineering](https://github.com/afrexai/git-engineering)

---

*最后更新：2026-05-25*