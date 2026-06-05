# @Transactional 统一策略文档

> **语言**: [English](../transactional-strategy.md) | **简体中文** | [繁體中文](./zh-Hant-TW/transactional-strategy.md)

> 本文档规定 JobCopilot 后端 `application` 模块中 Spring `@Transactional` 的使用规范。
> 目标：消除事务边界泄漏、嵌套事务污染、长事务阻塞等隐患。
>
> 版本：2026-05-25 | 分支：`sanitize-for-oss`

---

## 1. 核心原则

| # | 原则 | 理由 |
|---|---|---|
| 1 | **命令用 `@Transactional`，查询用 `@Transactional(readOnly = true)`** | 利用只读事务优化（Hibernate 不脏检查、不 flush），减少 DB 写锁。 |
| 2 | **类级只读，方法级覆写** | 读多写少的 Service，在类上声明 `@Transactional(readOnly = true)`，写方法单独标记 `@Transactional`。减少重复注解。 |
| 3 | **禁止在事务内做网络 I/O（HTTP、MQ、外部文件）** | 长事务 = 长连接占用，降低吞吐量；外部失败导致事务回滚，与业务语义不符。 |
| 4 | **跨聚合写操作各自独立事务** | 不同聚合根（Job、Resume、Conversation…）的写操作不应共享事务；用 `Propagation.REQUIRES_NEW` 或拆分到独立 Service。 |
| 5 | **Scheduler / 异步任务必须独立事务边界** | 定时任务运行周期长，默认事务应最小化；涉及 Outbox 模式时，relay 与 cleanup 分事务。 |
| 6 | **所有 `@Transactional` 必须显式声明 `timeout`** | 防止慢查询、锁等待或意外网络 I/O 导致无限长事务占用连接池。纯 DB 操作 30s；含 batch/sync 的 60s。 |
| 7 | **聚合根必须有乐观锁（`@Version`）** | 防止并发写场景下的丢失更新。当前已应用到 `Job`、`ResumeGroup`、`Conversation`、`JobMatchResult`、`ApplicationTracking`、`User`、`MatchingModel`。 |

---

## 2. 注解矩阵

| 场景 | 推荐注解 | 当前状态 |
|---|---|---|
| **纯查询**（get/list/find/search） | 类级 `@Transactional(readOnly = true)` + 无额外方法注解 | ✅ 已统一 |
| **单聚合根写**（save/update/delete） | 方法级 `@Transactional` | ✅ 已统一 |
| **批量写**（batchUpsert） | 方法级 `@Transactional` | ✅ 已统一 |
| **写后需发 MQ / HTTP** | **事务内只写 DB**，发 MQ 在事务提交后（事务监听器、ApplicationEvent、或 Outbox） | ⚠️ 见 §3.1 |
| **失败日志持久化**（不干扰主事务） | `@Transactional(propagation = REQUIRES_NEW, timeout = 30)` | ✅ 已统一 |
| **Scheduler（Outbox relay）** | 读取 `@Transactional(readOnly = true, timeout = 30)` + 发送后更新状态用 `REQUIRES_NEW, timeout = 30` | ⚠️ 见 §3.2 |
| **Scheduler（cleanup）** | `@Transactional(timeout = 30)`（短事务，批量删除） | ✅ 已统一 |

---

## 3. 当前已知问题

### 3.1 [已修复] 事务内发送 MQ —— `JobSubmissionService.submit()`

**文件**：`app/.../JobSubmissionService.java:31`

```java
@Transactional
public JobResponse submit(UUID userId, SubmitJobRequest request) {
    ...
    jobRepository.save(job);          // ← DB 写
    aiMessagePublisherPort.sendJobForParsing(...);  // ← 网络 I/O（RabbitMQ / HTTP）
    ...
}
```

**风险**：
1. MQ 发送失败 → 整个事务回滚 → 已 save 的 Job 丢失，但用户已收到提交成功响应。
2. MQ 发送成功但事务回滚 → 消息已发出但 DB 无记录 → 消费者找不到 Job。

**状态**：✅ **已修复** — 无需代码修改。`AiMessagePublisherPort` 实际已采用 **Outbox 模式**（事务内仅写入 `outbox` 表；`OutboxRelayScheduler` 轮询后发送至 RabbitMQ）。审计期间确认。

---

### 3.2 [已修复] Scheduler 事务边界模糊

**文件**：`app/.../OutboxRelayScheduler.java:38`

```java
@Transactional
public void relayPendingMessages() { ... }
```

**风险**：整个 relay 过程在一个事务内：读取 pending → 逐个发送 → 更新状态。如果消息量大，事务极长。

**状态**：✅ **已修复**，commit `618757f6`。

**已应用的修复**：
- 读取 pending 用 `@Transactional(readOnly = true)`。
- 提取 `OutboxRelayTransactionService`，使用 `@Transactional(propagation = Propagation.REQUIRES_NEW)`；每条消息的 RabbitMQ `convertAndSend` + `outboxMessageRepository.save` 运行在独立事务中，防止单条失败或网络阻塞导致整批回滚。

---

### 3.3 [已修复] `JobApplicationService.submitJob()` 与 `JobSubmissionService.submit()` 双层 `@Transactional` 冗余

**文件**：
- `JobApplicationService.java:53`（`@Transactional`）
- `JobSubmissionService.java:31`（`@Transactional`）

`JobApplicationService.submitJob()` 调用 `JobSubmissionService.submit()`，两者都有 `@Transactional`。Spring 默认 `REQUIRED`，内层会加入外层事务。虽然功能正常，但**架构上冗余**：事务边界应在最外层统一声明，内层纯业务逻辑不应重复注解。

**状态**：✅ **已修复**，commit `618757f6`。

**已应用的修复**：去掉 `JobSubmissionService.submit()` 的 `@Transactional`，事务由 `JobApplicationService.submitJob()` 统一控制。
---

### 3.4 `MatchingApplicationService.startJobMatch()` 含向量生成

**文件**：`MatchingApplicationService.java:58`

**状态**：✅ **已修复**

**问题**：`startJobMatch()` 持有 `@Transactional`，内部在向量缺失时同步调用 `vectorGenerationPort.generateAndSaveVector()`，该端口最终通过 HTTP 调用 Embedding 服务。长 HTTP 调用阻塞数据库事务，且失败时会导致整个匹配请求回滚。

**修复方案**：
- 提取 `MatchTransactionService`（包级 Bean），仅封装 recall + persist + outbox-write 等纯 DB 操作，声明 `@Transactional`。
- `MatchingApplicationService.startJobMatch()` 去掉 `@Transactional`，负责：
  1. 查询 model version、resume vector（事务外）。
  2. 向量缺失时，在事务外触发 on-demand 向量生成（HTTP 调用）。
  3. 向量就绪后，委托 `matchTransactionService.execute()` 进入短事务完成 recall 与 ranking 请求投递。
- 事务边界：HTTP 调用（向量生成）在事务外；轻量 DB 操作在事务内。

**修改文件**：
- `MatchingApplicationService.java`：拆分为 `MatchTransactionService` + `MatchingApplicationService`
- `MatchingApplicationServiceTest.java`：更新测试以适配拆分后的结构

---

### 3.5 [已修复] `JobApplicationService.handleJobProcessResult()` — 事务内向量生成 HTTP 调用

**文件**：`app/.../JobApplicationService.java`

```java
@Transactional
public void handleJobProcessResult(AiResultEvent event) {
    ...
    vectorGenerationService.generateForJob(...);  // ← HTTP 调用（Embedding 服务）
    jobDatasetSyncService.sync(job, event);       // ← DB 写
    jobRepository.save(job);
}
```

**风险**：`vectorGenerationService.generateForJob()` 触发对 Embedding 服务的 HTTP 调用。若 Embedding 服务响应慢或超时，数据库连接将在整个期间被占用，耗尽连接池。

**状态**：✅ **已修复**。

**已应用的修复**：
- 提取 `JobResultTransactionService`（包级 Bean），仅封装纯 DB 操作（`markCompleted`/`markFailed` + `sync` + `save`），声明 `@Transactional(timeout = 60)`。
- `JobApplicationService.handleJobProcessResult()` 去掉 `@Transactional`。短数据库事务完成后，向量生成在**事务外**执行。
- `JobApplicationService` 及 `application` 模块中所有剩余的 `@Transactional` 方法现已携带显式 `timeout` 值（纯 DB 写操作 30s，batch/sync 操作 60s）。

**修改文件**：
- `JobApplicationService.java`：重构 `handleJobProcessResult()`；为所有 `@Transactional` 添加 `timeout`
- `JobResultTransactionService.java`：新增文件（包级可见）

---

## 4. 最佳实践速查

```java
/**
 * 读多写少的 Service：类级只读，写方法覆写。
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    public JobResponse getJob(...) { ... }          // 继承 readOnly

    @Transactional(timeout = 30)
    public JobResponse updateJob(...) { ... }    // 带乐观锁的写入

    /** 聚合根乐观锁。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** 领域模型暴露 version 用于冲突检测。 */
    public void markCompleted(ParsedJobContent content) {
        if (this.status != JobStatus.PARSING) {
            throw new IllegalStateException("Job must be PARSING to complete.");
        }
        this.status = JobStatus.COMPLETED;
        this.parsedContent = content;
    }
}

/**
 * 纯命令 Service：类级不写 readOnly。
 */
@Service
@RequiredArgsConstructor
public class JobSubmissionService {
    // 不声明类级 @Transactional，由调用方控制事务
}

/**
 * 独立事务（失败日志），显式超时。
 */
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
public void saveFailedVector(...) { ... }
```

---

## 5. 检查清单（Code Review 用）

- [ ] 查询方法是否误用 `@Transactional`（未加 `readOnly = true`）？
- [ ] 写方法内部是否有 HTTP / MQ / 文件 I/O？若有，是否移到事务外？
- [ ] 是否有 `@Transactional` 嵌套冗余（两层 Service 都声明）？
- [ ] Scheduler / 异步任务事务是否最小化？
- [ ] 是否出现 `REQUIRES_NEW` 滥用（非日志/补偿类场景）？
- [ ] 每个 `@Transactional` 是否都声明了显式 `timeout`（不依赖默认值）？
- [ ] 每个聚合根实体是否都有 `@Version` 乐观锁？

---

## 6. 附录：当前 `@Transactional` 全量清单

### 6.1 类级注解

| 类 | 注解 | 说明 |
|---|---|---|
| `ConversationApplicationService` | `@Transactional(readOnly = true)` | 类级只读，个别写方法覆写 |
| `ConversationQueryService` | `@Transactional(readOnly = true)` | 纯查询 |
| `AuthApplicationService` | `@Transactional(readOnly = true)` | 类级只读，register/login 覆写 |
| `ProfileApplicationService` | `@Transactional(readOnly = true)` | 类级只读，update 覆写 |
| `ModelManagementApplicationService` | — | 两个 get 用 readOnly，switch 用读写 |

### 6.2 写操作（`@Transactional` 无 readOnly）

| 类 | 方法 | 备注 |
|---|---|---|
| `ResumeUploadHandler.upload` | 上传简历 | |
| `ResumeApplicationService.handleUpload` | 处理上传 | |
| `ResumeApplicationService.handleEdit` | 编辑 | |
| `ResumeApplicationService.handleCreateVersion` | 创建版本 | |
| `ResumeApplicationService.handleDelete` | 删除组 | |
| `ResumeApplicationService.handleDeleteVersion` | 删除版本 | |
| `ResumeApplicationService.handleActivateVersion` | 激活版本 | |
| `ResumeApplicationService.handleParseResult` | 解析结果 | |
| `ResumeVersionChainManager.createVersion` | 版本链 | |
| `ResumeDeletionService.deleteGroup` | 删除组 | |
| `ResumeDeletionService.deleteVersion` | 删除版本 | |
| `ResumeParseResultHandler.handle` | 解析结果 | |
| `FailedVectorPersistenceService.saveFailedVector` | `REQUIRES_NEW` | ✅ 正确使用 |
| `JobVectorBatchService.batchUpsert` | 批量 upsert | |
| `ResumeVectorBatchService.batchUpsert` | 批量 upsert | |
| `ConversationApplicationService.createConversation` | 创建对话 | 覆写类级 readOnly |
| `ConversationApplicationService.sendMessage` | 发消息 | |
| `ConversationApplicationService.saveAiReply` | 保存 AI 回复 | 两个重载 |
| `ConversationApplicationService.uploadAttachment` | 上传附件 | 含文件流 |
| `ConversationApplicationService.closeConversation` | 关闭对话 | |
| `ConversationApplicationService.deleteConversation` | 删除对话 | |
| `AuthApplicationService.registerByEmail` | 注册 | |
| `AuthApplicationService.loginByGoogle` | Google 登录 | |
| `ProfileApplicationService.updateProfile` | 更新资料 | |
| `ProfileApplicationService.updateAvatar` | 更新头像 | |
| `OutboxCleanupScheduler.cleanup` | 清理 Outbox | |
| `OutboxRelayScheduler.relayPendingMessages` | ⚠️ 长事务 | |
| `ModelManagementApplicationService.switchActiveModel` | 切换模型 | |
| `MatchingApplicationService.startJobMatch` | ⚠️ 需确认含外部调用？ | |
| `MatchingApplicationService.saveMatchResult` | 保存结果 | |
| `TrackingApplicationService.createTracking` | 创建追踪 | |
| `TrackingApplicationService.updateTracking` | 更新追踪 | |
| `TrackingApplicationService.deleteTracking` | 删除追踪 | |
| `JobScoringContextLoader.load` | 加载评分上下文 | |
| `JobScoringResultSaver.save` | 保存评分结果 | |
| `JobSubmissionService.submit` | ⚠️ 事务内发 MQ | 见 §3.1 |
| `JobApplicationService.submitJob` | 提交职位 | |
| `JobApplicationService.handleJobProcessResult` | 处理结果 | |
| `JobApplicationService.updateJob` | 更新职位 | |
| `JobApplicationService.deleteJob` | 删除职位 | |
| `JobApplicationService.trackUserAction` | 追踪行为 | |

### 6.3 只读操作（`@Transactional(readOnly = true)`）

| 类 | 方法 |
|---|---|
| `ResumeApplicationService.getGroup` | |
| `ResumeApplicationService.listUserGroups` | |
| `ResumeApplicationService.getVersion` | |
| `ConversationApplicationService` | （类级） |
| `ConversationQueryService` | （类级） |
| `AuthApplicationService` | （类级，register/login 覆写） |
| `ProfileApplicationService` | （类级，update 覆写） |
| `ModelManagementApplicationService.getActiveRecallModel` | |
| `ModelManagementApplicationService.getActiveRankerModel` | |
| `MatchingApplicationService.getMatchResult` | |
| `MatchingApplicationService.listMatchHistory` | |
| `TrackingStatsService.calculateStats` | |
| `TrackingApplicationService.getTracking` | |
| `TrackingApplicationService.listTrackings` | |
| `JobApplicationService.getJob` | |
| `JobApplicationService.listJobs` | |
| `JobApplicationService.getScoreHistory` | |

---

## 7. 修订历史

| 日期 | 修订人 | 内容 |
|---|---|---|
| 2026-05-25 | AI Architecture Audit | 为 `Job` 聚合根添加 `@Version` 乐观锁（§1.7、§6.4）。修复 `handleJobProcessResult` 事务内 HTTP 调用问题（§3.5）。所有 `@Transactional` 方法统一显式 `timeout`。 |
