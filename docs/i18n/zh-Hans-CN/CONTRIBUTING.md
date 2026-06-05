# 贡献指南

> **语言**: [English](../../../CONTRIBUTING.md) | **简体中文** | [繁體中文](../zh-Hant-TW/CONTRIBUTING.md)

感谢您对 JobCopilot 的兴趣！本文档提供指南和说明，帮助您快速上手。

---

## 目录

- [行为准则](#行为准则)
- [快速开始](#快速开始)
- [开发环境搭建](#开发环境搭建)
- [项目结构](#项目结构)
- [分支策略](#分支策略)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [架构指南](#架构指南)
- [测试要求](#测试要求)
- [代码风格](#代码风格)
- [文档](#文档)
- [发布流程](#发布流程)
- [社区](#社区)

---

## 行为准则

本项目遵循尊重、建设性协作的标准。参与即表示您同意：

- 在所有互动中保持尊重和包容
- 优雅地接受建设性批评
- 关注对社区和项目最有益的事项
- 对其他社区成员展现同理心

---

## 快速开始

### 前置条件

| 组件 | 最低版本 | 推荐版本 |
|-----------|----------------|-------------|
| Java      | 21             | 21 (LTS)    |
| Maven     | 3.9.6          | 3.9.9       |
| Node.js   | 20.11.0        | 20.x LTS    |
| Python    | 3.11           | 3.11+       |
| Docker    | 24.0.0         | 27.x        |
| Docker Compose | 2.20.0    | 2.29+       |

> 验证环境：`java -version`、`mvn -v`、`node -v`、`python3 --version`、`docker --version`

### Fork 与克隆

```bash
# 在 GitHub 上 Fork 仓库，然后克隆您的 Fork
git clone <your-fork-url>
cd <repository-name>

# 添加上游远程仓库
git remote add upstream <upstream-repository-url>
```

---

## 开发环境搭建

### 方案 A：Docker Compose（推荐首次贡献者使用）

```bash
cp .env.example .env
# 编辑 .env 配置您的参数
docker compose --env-file .env up -d --build
```

### 方案 B：本地开发

```bash
# 1. 启动共享基础设施服务
cp .env.example .env
# 编辑 .env 配置您的本地参数
docker compose --env-file .env up -d postgres redis rabbitmq minio

# 2. 后端
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -pl app

# 3. 前端（新终端）
cd frontend
npm install
npm run dev

# 4. AI 服务（新终端，可选）
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### 环境变量

将 `.env.example` 复制为 `.env` 并配置：
- 数据库凭证（PostgreSQL + pgvector）
- MinIO / S3 兼容存储
- RabbitMQ 连接
- LiteLLM 提供商 API 密钥，例如 Gemini、OpenAI 或 Anthropic
- Google OAuth 凭证

详见 `docs/deployment/` 详细配置参考。

---

## 项目结构

```
├── backend/           # Java / Spring Boot（DDD 六边形架构）
│   ├── app/           # 应用层（Service、Scheduler）
│   ├── domain/        # 领域层（Entity、Value Object、Port）
│   ├── api/           # API 层（DTO、Command、Query）
│   ├── infrastructure/# 适配器（DB、MQ、HTTP、文件存储）
│   ├── trigger/       # REST Controller、WebSocket、事件监听器
│   └── types/         # 共享类型与常量
├── frontend/          # TypeScript / React / Vite / TailwindCSS
├── ai-service/        # Python / FastAPI（Embedding、LLM 推理）
├── docs/              # 文档（英文 + i18n）
└── .github/           # CI/CD、模板、Dependabot
```

### 架构原则

- **领域驱动设计（DDD）**：业务逻辑位于 `domain/`；无框架依赖
- **六边形架构（Ports & Adapters）**：领域定义端口；基础设施实现适配器
- **Outbox 模式**：所有异步消息通过数据库 outbox 表确保可靠性
- **CQRS**：在适用场景分离命令与查询职责

---

## 分支策略

我们采用适合开源协作的轻量主干开发流程：

| 分支 | 用途 | 保护规则 |
|--------|---------|------------|
| `main` | 稳定开发分支和发布来源 | 受保护；需要 PR + 审查 |
| `feature/*` | 新功能 | 短生命周期分支；通过 PR 合并到 `main` |
| `fix/*` | Bug 修复 | 短生命周期分支；通过 PR 合并到 `main` |
| `docs/*` | 仅文档变更 | 短生命周期分支；通过 PR 合并到 `main` |
| `chore/*` | 维护、依赖、工具链变更 | 短生命周期分支；通过 PR 合并到 `main` |
| `release/v*` | 可选的发布稳定分支 | 仅在维护者准备发布时创建 |

### 工作流程

```bash
# 开始新功能
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name

# 工作、提交、推送
git add .
git commit -m "feat(matching): add vector caching for recall"
git push origin feature/your-feature-name

# 针对 main 开启 Pull Request
```

---

## 提交规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范：

| 类型 | 描述 | 示例 |
|------|-------------|---------|
| `feat` | 新功能 | `feat(auth): add Google OAuth login` |
| `fix` | Bug 修复 | `fix(tx): resolve nested transaction in matching` |
| `docs` | 仅文档 | `docs(deploy): add Kubernetes deployment guide` |
| `style` | 代码风格（格式化） | `style(frontend): apply prettier to components` |
| `refactor` | 重构 | `refactor(domain): extract JobValidator from service` |
| `perf` | 性能优化 | `perf(embedding): batch vector generation` |
| `test` | 添加或修正测试 | `test(matching): add MatchTransactionService tests` |
| `chore` | 维护 / 依赖 | `chore(deps): bump Spring Boot to 3.5.7` |
| `ci` | CI/CD 变更 | `ci: add OWASP dependency check` |
| `build` | 构建系统变更 | `build: configure maven-enforcer-plugin` |
| `revert` | 回滚提交 | `revert: rollback vector cache (regression)` |

### 作用域规范

使用组件级作用域：`auth`、`job`、`resume`、`matching`、`embedding`、`conversation`、`tracking`、`domain`、`infrastructure`、`frontend`、`ai`、`deploy`、`docs`。

### 破坏性变更

在描述前加 `!` 或添加 `BREAKING CHANGE:` 脚注：

```
feat(api)!: remove deprecated v1 endpoints

BREAKING CHANGE: /api/v1/* 端点已移除。请迁移至 /api/v2/*。
```

---

## Pull Request 流程

### 提交前

1. **更新分支**：`git pull upstream main`
2. **本地运行质量检查**：
   ```bash
   # 后端
   cd backend && mvn clean verify

   # 前端
   cd frontend && npm run lint && npm run test:run

   # AI 服务
   cd ai-service && ruff check . && pytest
   ```
3. **为新逻辑编写或更新测试**
4. **如果用户可见行为变更，更新文档**
5. **审查您的 diff** — 保持变更聚焦且最小化

### PR 模板

PR 必须包含：

- **What**：变更的清晰描述
- **Why**：动机和上下文（关联 issue：`Fixes #123`）
- **How**：关键实现决策
- **Testing**：如何验证变更
- **Checklist**：确认以下所有项

### 审查标准

- [ ] 代码遵循 DDD 六边形架构
- [ ] `domain/` 模块无框架依赖
- [ ] 新增/更新的测试覆盖修改行 ≥60%
- [ ] 无 `@Transactional` 与网络 I/O（HTTP、MQ、文件）混用
- [ ] ESLint / Prettier 通过（前端）
- [ ] Maven 构建含测试通过
- [ ] 如有适用，文档已更新
- [ ] 提交信息遵循 Conventional Commits

### 审查流程

1. 作者针对 `main` 开启 PR
2. CI 检查必须通过（构建、测试、Lint、安全扫描）
3. 至少需要一个维护者审查
4. 使用 fixup 提交处理审查反馈
5. 维护者 squash 合并

---

## 架构指南

### DDD 分层规则

```
┌─────────────────────────────────────┐
│  trigger  (Controllers, WebSocket)   │  ◄── HTTP / WebSocket 入口
├─────────────────────────────────────┤
│  app      (Application Services)     │  ◄── 编排、事务边界
├─────────────────────────────────────┤
│  domain   (Entities, Ports, VO)    │  ◄── 纯业务逻辑
├─────────────────────────────────────┤
│  infra    (Adapters, Repositories) │  ◄── DB、MQ、HTTP、存储
└─────────────────────────────────────┘
```

| 规则 | 执行方式 |
|------|-------------|
| `domain/` 零 Spring / Hibernate 导入（`javax.validation` 除外） | ArchUnit 测试：`noClasses().that().resideInAPackage("..domain..")..should().dependOnClassesThat().resideInAPackage("org.springframework..")` |
| 领域接口（Ports）位于 `domain/**/port/` | 代码审查 |
| Application Services 持有 `@Transactional`，Domain Services 不持有 | 检查清单 |
| 基础设施适配器实现领域端口 | 编译期 |

### 事务安全

- **命令**：`@Transactional`（读写）
- **查询**：`@Transactional(readOnly = true)`
- **事务内不做网络 I/O**：HTTP 调用、MQ 发送、文件上传必须在 `@Transactional` 外
- **Outbox 模式**：所有异步消息先写入 `outbox` 表；scheduler 中继

详见 `docs/transactional-strategy.md` 完整策略。

---

## 测试要求

### 后端（Java）

| 测试类型 | 工具 | 最低覆盖率 |
|-----------|------|-----------------|
| 单元测试 | JUnit 5 + Mockito | 60% 指令 / 行，40% 分支 |
| 架构测试 | ArchUnit | 100%（必须通过） |
| 集成测试 | `@SpringBootTest` | 仅关键流程 |

```bash
# 运行所有后端测试含覆盖率
cd backend && mvn clean verify

# 运行特定模块测试
cd backend/app && mvn test
```

### 前端（TypeScript）

| 测试类型 | 工具 | 要求 |
|-----------|------|-------------|
| 单元测试 | Vitest + React Testing Library | 所有含逻辑组件 |
| E2E 测试 | Playwright（规划中） | 关键用户旅程 |

```bash
cd frontend
npm run test:run        # 一次性运行
npm run test:coverage   # 含覆盖率报告
```

### AI 服务（Python）

| 测试类型 | 工具 | 要求 |
|-----------|------|-------------|
| 单元测试 | pytest | 所有服务函数 |
| 代码检查 | ruff | 必须通过 |

```bash
cd ai-service
pytest --cov=app --cov-report=term-missing
```

---

## 代码风格

### Java

- **格式化器**：使用 Spring Boot 默认风格（4 空格，120 字符行宽）
- **Lombok**：允许在 `app/` 和 `infra/`；**不允许**在 `domain/`
- **导入**：禁止通配符导入；`Assertions`、`Mockito` 使用静态导入
- **空安全**：使用 `Optional<>`；避免从领域方法返回 `null`

### TypeScript / React

- **格式化器**：Prettier（配置在 `frontend/prettier.config.js`）
- **代码检查器**：ESLint with TypeScript、React Hooks、React Refresh 规则
- **组件**：函数组件 + Hooks；不使用类组件
- **样式**：TailwindCSS + `class-variance-authority` 做组件变体

```bash
# 自动修复前端问题
cd frontend
npm run lint            # 检查
npm run format          # 格式化
```

### Python

- **格式化器**：Black 兼容（通过 `ruff format`）
- **代码检查器**：ruff 含 isort、flake8、pydocstyle 规则
- **类型注解**：所有函数签名必须含类型注解

```bash
cd ai-service
ruff check .            # 检查
ruff format .           # 格式化
```

---

## 文档

- **用户可见变更**：更新 `docs/` 英文 + 中文（简体 + 繁体）版本
- **架构决策**：添加条目到 `docs/adr/`（Architecture Decision Records）
- **API 变更**：OpenAPI 注解自动更新；通过 `mvn spring-boot:run` + `/swagger-ui.html` 验证
- **部署变更**：更新 `docs/deployment/` 和 `.env.example`

---

## 发布流程

1. **版本号更新**：更新 `backend/pom.xml` 和 `frontend/package.json` 的 `version`
2. **更新日志**：如果项目维护 `CHANGELOG.md`，用 Conventional Commits 摘要更新它
3. **打标签**：`git tag -a v1.x.x -m "Release v1.x.x"`
4. **构建**：CI 构建 Docker 镜像并推送至仓库
5. **部署**：用新镜像更新生产环境

---

## 社区

- **Issues**：使用 GitHub Issues 提交 Bug 报告和功能请求
- **Discussions**：使用 GitHub Discussions 提问和交流想法
- **安全漏洞**：私信维护者报告安全漏洞

---

## 有疑问？

如有任何不清楚的地方，请开启 [GitHub Discussion](https://github.com/<owner>/<repo>/discussions) 或在 Issue 中提问。我们很乐意帮助。

**感谢贡献！🚀**
