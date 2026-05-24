# 为 JobCopilot ResumeAssistant 做出贡献

> **Languages:** [English](../../CONTRIBUTING.md) | 简体中文 (current) | [繁體中文](../zh-Hant-TW/CONTRIBUTING.md)

本项目基于六边形架构（端口与适配器），正从学习项目转向开源产品。欢迎贡献。

## 目录

- [快速开始](#快速开始)
- [开发环境](#开发环境)
- [分支策略](#分支策略)
- [提交规范](#提交规范)
- [代码风格](#代码风格)
- [测试要求](#测试要求)
- [拉取请求流程](#拉取请求流程)
- [架构合规性](#架构合规性)
- [文档](#文档)
- [发布流程](#发布流程)
- [社区](#社区)

---

## 快速开始

1. 在 GitHub 上**Fork**本仓库。
2. 在本地**Clone**您的 fork：
   ```bash
   git clone https://github.com/YOUR_USERNAME/ser594_Team6-ResumeAssistant.git
   cd ser594_Team6-ResumeAssistant
   ```
3. **设置 upstream** 远程：
   ```bash
   git remote add upstream https://github.com/original-owner/ser594_Team6-ResumeAssistant.git
   ```
4. 按照我们的[分支命名规范](#分支策略)创建一个分支。

---

## 开发环境

### 前置条件

| 组件 | 所需版本 |
|-----------|----------------|
| Java | 21 (LTS) |
| Maven | 3.9+ |
| Node.js | 20+ (前端) |
| Python | 3.11+ (AI 服务) |
| Docker & Docker Compose | 最新稳定版 |
| PostgreSQL | 15+ (需 pgvector 扩展) |
| MinIO | 最新版 (对象存储) |
| RabbitMQ | 3.12+ (消息队列) |

### 快速启动

```bash
# 1. 启动基础设施服务
docker compose -f docker-compose.yml.example up -d postgres minio rabbitmq

# 2. 配置环境
cp .env.example .env
# 根据您的本地设置编辑 .env

# 3. 构建后端
cd backend && mvn clean install -DskipTests

# 4. 启动后端服务
cd backend/trigger && mvn spring-boot:run

# 5. 启动前端（另一个终端）
cd frontend && npm install && npm run dev

# 6. 启动 AI 服务（另一个终端）
cd ai-service && pip install -r requirements.txt && uvicorn app.main:app --reload
```

---

## 分支策略

我们使用 **GitHub Flow** —— 简单、轻量，针对持续交付优化。

```
main (受保护)
  ↑
feat/PROJ-123-user-authentication
  ↑
fix/PROJ-456-login-timeout
  ↑
hotfix/PROJ-789-payment-crash
```

### 分支命名规范

```
{type}/{ticket-id}-{short-description}
```

| 类型 | 用途 |
|------|---------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `hotfix` | 生产环境紧急修复 |
| `chore` | 维护、依赖更新 |
| `docs` | 仅文档变更 |
| `refactor` | 代码重构，无行为变更 |
| `test` | 测试添加或修复 |
| `perf` | 性能改进 |
| `ci` | CI/CD 配置 |
| `style` | 仅代码格式化 |

**规则：**
- 全部小写，空格用连字符替代
- 类型前缀后最多 50 个字符
- 有 ticket/issue 编号时必须包含
- 合并后分支自动删除

### 分支生命周期目标

| 分支类型 | 目标生命周期 | 最大生命周期 |
|-------------|----------------|------------------|
| Feature | 1-3 天 | 5 天 |
| Bug fix | <1 天 | 2 天 |
| Hotfix | <4 小时 | 1 天 |

**规则：** 如果分支超过最大生命周期，必须拆分为更小的 PR。

---

## 提交规范

我们强制执行 **Conventional Commits**。每条提交信息必须遵循此格式：

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
| `perf` | 性能改进 | `perf(vector): optimize cosine similarity calculation` |
| `refactor` | 无行为变更 | `refactor(job): extract ScreenshotValidator from ApplicationService` |
| `docs` | 文档 | `docs(deploy): add Docker Desktop troubleshooting` |
| `test` | 仅测试 | `test(auth): add SSO edge cases` |
| `chore` | 构建/工具 | `chore(deps): bump Spring Boot to 3.5.8` |
| `ci` | CI/CD 变更 | `ci: add JaCoCo coverage threshold check` |
| `style` | 仅格式化 | `style: apply Spotless formatting` |
| `revert` | 回退之前提交 | `revert: feat(api): add resume upload endpoint` |

### 范围参考

| 范围 | 区域 |
|-------|------|
| `api` | API 层（DTO、Facade） |
| `app` | 应用层（ApplicationServices） |
| `domain` | 领域层（实体、值对象、端口） |
| `infra` | 基础设施层（适配器、配置） |
| `trigger` | 触发层（控制器、事件监听器） |
| `db` | 数据库（迁移、Schema） |
| `deploy` | 部署配置 |
| `docs` | 文档 |
| `ci` | CI/CD 流水线 |
| `frontend` | 前端应用 |
| `ai` | AI 服务（Python） |

### 质量规则

1. **原子提交** —— 每次提交一个逻辑变更
2. **祈使语气** —— "add feature" 而非 "added feature"
3. **主题 ≤ 72 字符** —— 必须能放入 `git log --oneline`
4. **正文每行 72 字符** —— 终端可读
5. **引用 Issue** —— `Fixes #123` 或 `Refs PROJ-456`
6. **main 上不允许 WIP 提交** —— 先 squash 或交互式 rebase
7. **破坏性变更必须显式声明：**
   ```
   feat(api)!: change auth header format
   
   BREAKING CHANGE: Authorization header now requires "Bearer " prefix.
   Migration: Update all API clients to include "Bearer " before token.
   ```

---

## 代码风格

### Java（后端）

- **格式化器：** Spotless Maven 插件（Google Java Format）
- **执行：** `cd backend && mvn spotless:apply`
- **检查：** `cd backend && mvn spotless:check`

### TypeScript（前端）

- **格式化器：** Prettier
- **Linter：** ESLint
- **执行：** `cd frontend && npm run lint:fix`

### Python（AI 服务）

- **格式化器：** Black
- **Linter：** Ruff
- **执行：** `cd ai-service && ruff check . && black .`

### 架构规则（强制）

我们的后端遵循 **六边形架构（端口与适配器）**。违规将在代码审查中被拒绝。

| 层级 | 可依赖 | 禁止依赖 |
|-------|-------------|-------------------|
| `trigger` | `app`, `api` | `domain`, `infrastructure` |
| `app` | `domain`, `api` | `infrastructure`, `trigger` |
| `domain` | 仅 `types` | `app`, `infrastructure`, `trigger`, `api` |
| `infrastructure` | `domain`, `app`, `api` | — |
| `api` | 仅 `types` | `app`, `domain`, `infrastructure`, `trigger` |
| `types` | 无（纯共享类型） | 任何层级 |

**关键约束：**
- ApplicationService 不得超过 **150 行** —— 拆分为 DomainService 或用例
- `app` 或 `domain` 中禁止出现 `RestTemplate` / `HttpClient` / `JdbcTemplate`
- `domain` 中禁止 Spring 注解（`@Component`, `@Service`, `@Autowired`）
- 外部 API 响应禁止使用 `Map<String, Object>` —— 使用严格 DTO
- 领域实体必须有**行为方法**，不能只有 getter/setter

我们使用 **ArchUnit** 测试自动执行这些规则。运行：
```bash
cd backend && mvn test -pl app -Dtest="*ArchitectureTest*"
```

---

## 测试要求

### 最低覆盖率（CI 强制执行）

| 层级 | 最低覆盖率 |
|-------|-----------------|
| `domain` | 80% |
| `app` | 70% |
| `infrastructure` | 50% |
| `trigger` | 60% |

### 所需测试类型

1. **单元测试** —— JUnit 5 + Mockito + AssertJ
   ```bash
   cd backend && mvn test
   ```

2. **架构测试** —— ArchUnit
   ```bash
   cd backend && mvn test -Dtest="*ArchitectureTest*"
   ```

3. **集成测试** —— Spring Boot Test + Testcontainers
   ```bash
   cd backend && mvn verify -Pintegration-test
   ```

### 编写优质测试

- 测试**行为**，而非实现
- 测试名称使用 **Given-When-Then** 结构：
  ```java
  @Test
  void shouldRejectScreenshotLargerThan5MB() { ... }
  ```
- Mock 外部依赖；用真实对象测试领域逻辑
- 集成测试必须清理数据库状态（`@Transactional` 或 `@Sql`）

---

## 拉取请求流程

### 创建 PR 之前

1. Rebase 到最新 `main`：
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```
2. 运行完整检查套件：
   ```bash
   cd backend && mvn clean verify
   cd frontend && npm run lint && npm run test
   cd ai-service && ruff check . && pytest
   ```
3. 确保分支符合命名规范
4. Squash WIP 提交：`git rebase -i upstream/main`

### PR 模板

打开 PR 时模板会自动填充。填写所有部分：

- **What** —— 一句话描述
- **Why** —— 链接到 Issue 或解释问题
- **How** —— 技术方案和关键决策
- **Testing** —— 已执行测试清单
- **Screenshots** —— UI 变更时提供
- **Checklist** —— 自审清单

### PR 大小指南

| 大小 | 变更行数 | 预期审查时间 |
|------|--------------|---------------------|
| XS | <10 | 5 分钟 |
| S | 10-50 | 15 分钟 |
| M | 50-200 | 30 分钟 |
| L | 200-500 | 60 分钟 |

**规则：** >500 行的 PR 缺陷率高出 40%。积极拆分。

### 审查流程

| 变更类型 | 最少批准数 | 要求的审查者 |
|-------------|------------------|-------------------|
| Feature | 2 | 1 名领域专家 |
| Bug fix | 1 | 任何团队成员 |
| Hotfix | 1 | On-call + 负责人 |
| Refactor | 2 | 原作者（如可用） |
| Docs only | 1 | 任何人 |
| Dependency update | 1 | 安全意识审查者 |
| Database migration | 2 | DBA/资深 + 1 名开发 |

### 审查评论分类

为评论加前缀以明确意图：

| 前缀 | 含义 | 阻塞合并？ |
|--------|---------|--------------|
| `blocking:` | 合并前必须修复 | 是 |
| `suggestion:` | 考虑此改进 | 否 |
| `nit:` | 风格/格式偏好 | 否 |
| `question:` | 需要澄清 | 可能 |
| `praise:` | 做得好 | 否 |
| `thought:` | 长期考虑 | 否 |

---

## 架构合规性

### 六边形架构检查清单

提交 PR 前验证：

- [ ] ApplicationServices 仅做编排，不包含业务逻辑
- [ ] 领域实体有行为方法（不只是 getter/setter）
- [ ] `domain` 或 `app` 中无框架代码（`RestTemplate`, `JPA`, `RabbitTemplate`）
- [ ] 所有外部依赖通过 Port 接口接入
- [ ] DTO 位于 `api` 层，绝不泄漏到 `domain`
- [ ] ValueObjects 不可变且在构造时验证
- [ ] HTTP 调用或外部服务调用周围无 `@Transactional`

### 常见违规避免

❌ **上帝 ApplicationService** (>150 行) → 提取 DomainService 或用例  
❌ 外部 API 响应使用 `Map<String, Object>` → 定义严格 DTO  
❌ `@Transactional` 围绕 `RestTemplate` 调用 → 将 HTTP 调用移出事务  
❌ 控制器中包含业务逻辑 → 移到 ApplicationService 或 DomainService  
❌ 领域层使用 Spring 注解 → 使用纯 Java + 在应用层使用构造器注入  
❌ 仓储接口返回框架类型 → 仅返回领域类型  

---

## 文档

### 什么需要文档

所有面向用户或贡献者的文档必须提供 **三种语言**：
- **英语**（主文件，根目录）
- **简体中文** (`docs/i18n/zh-Hans-CN/`)
- **繁體中文** (`docs/i1../zh-Hant-TW/`)

| 文档 | 位置 | 需要 i18n? |
|----------|----------|---------------|
| README.md | `/` | 是 |
| CONTRIBUTING.md | `/` | 是 |
| CODE_OF_CONDUCT.md | `/` | 是 |
| CHANGELOG.md | `/` | 是 |
| 部署文档 | `docs/deployment/` | 是 |
| ADR | `docs/adr/` | 是 |
| API 文档 | 自动生成 (OpenAPI) | 否 |
| LICENSE | `/` | 否 |

### 架构决策记录 (ADRs)

任何重大架构决策在 `docs/adr/` 中创建 ADR：

```
docs/adr/
├── 001-hexagonal-architecture.md
├── 002-postgresql-pgvector.md
├── 003-rabbitmq-message-queue.md
└── 004-minio-object-storage.md
```

ADR 模板：
```markdown
# ADR-XXX: 标题

## 状态
Proposed / Accepted / Deprecated / Superseded

## 背景
我们要解决什么问题？

## 决策
我们决定了什么？

## 后果
正面和负面的结果。
```

---

## 发布流程

我们遵循 **语义化版本控制 (SemVer)**，使用 **release-please** 进行自动发布。

### 版本升级规则

| 变更类型 | 版本升级 | 示例 |
|-------------|-------------|---------|
| 破坏性 API 变更 | MAJOR | 删除端点、变更响应结构 |
| 新功能（向后兼容） | MINOR | 添加端点、新的可选字段 |
| Bug 修复 | PATCH | 修复计算错误、拼写错误 |

### 自动发布流水线

1. 使用 conventional commits 合并 PR 到 `main`
2. `release-please` 创建带有 changelog + 版本升级的发布 PR
3. 人工审查并合并发布 PR
4. 自动创建 Git 标签 `vX.Y.Z`
5. 发布带自动生成说明的 GitHub Release
6. 构建并推送 Docker 镜像 `ghcr.io/jobcopilot/resumeassistant:vX.Y.Z`

### 手动发布（仅限紧急情况）

```bash
# 仅用于自动化故障时的热修复
git tag -a v1.2.1 -m "Hotfix: resolve connection pool starvation"
git push upstream v1.2.1
```

---

## 社区

### 沟通渠道

- **Issues：** Bug 报告、功能请求、问题
- **Discussions：** 架构提案、一般问题
- **Security：** 安全漏洞请邮件 **security@jobcopilot.dev**（请勿公开创建 Issue）

### 获取帮助

1. 搜索现有 Issues 和 Discussions
2. 查阅 `docs/` 中的文档
3. 在 Discussions 中使用 `question` 标签提问
4. Bug 请使用 Bug Report Issue 模板

### 致谢

贡献者将在以下位置获得认可：
- 每次发布的 `CHANGELOG.md`
- `CONTRIBUTORS.md`（每季度更新）
- GitHub 发布说明

---

## 致谢

本贡献指南改编自：
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Semantic Versioning](https://semver.org/)
- [Contributor Covenant](https://www.contributor-covenant.org/)

感谢您让 JobCopilot 变得更好！
