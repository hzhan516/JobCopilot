# 后端测试用例评审报告
# Backend Test Case Review Report

**项目**: ResumeAssistant Backend  
**评审日期**: 2026-05-25  
**评审人**: testcase-reviewer Skill  
**模块覆盖**: domain / app / infrastructure / trigger  

---

## 📊 总体评价

| 指标 | 数值 |
|:---|:---|
| 测试文件总数 | 62 个 |
| 生产代码文件总数 | ~280 个 |
| 测试覆盖率（文件级） | ~22% |
| 综合评分 | **72/100 (C 级)** |
| 通过条件 | 高严重问题清零 且 评分 ≥ 75 ❌ |

---

## 🎯 六大维度得分

| 维度 | 得分 | 满分 | 关键问题 |
|:---|:---|:---|:---|
| **完整性** | 18/30 | 30 | 大量业务类、配置类、异常路径无测试覆盖 |
| **准确性** | 20/25 | 25 | 多数用例步骤清晰，但部分断言过于宽泛（assertNotNull 为主） |
| **有效性** | 8/15 | 15 | 缺乏边界值、等价类、状态转换等设计方法的系统性应用 |
| **可执行性** | 8/10 | 10 | 测试数据可构造，但存在对外部服务的真实依赖 |
| **规范性** | 7/10 | 10 | 命名基本规范，但缺少 @DisplayName、需求关联标识 |
| **可维护性** | 6/10 | 10 | 原子性不足（部分测试验证多个功能点）、独立性受数据依赖影响 |

---

## 🔍 各层测试覆盖现状

| 层级 | 生产文件 | 测试文件 | 覆盖度 | 核心缺口 |
|:---|:---|:---|:---|:---|
| **Domain** | 91 | 10 | 11% | 实体行为、领域服务、值对象、异常 |
| **Application** | 76 | 30 | 39% | Facade 完整覆盖，但 Service 类大量缺失 |
| **Infrastructure** | 97 | 14 | 14% | 适配器、配置、存储实现 |
| **Trigger** | 16 | 8 | 50% | Controller、安全过滤器、异常处理器 |
| **Integration** | — | 4 | — | 集成测试覆盖 Auth、Resume |

---

## 🚨 高严重问题清单（必须修改）

### 1. 【原子性/独立性】JobApplicationServiceTest 混合验证多个功能点
- **文件**: `JobApplicationServiceTest.java`
- **问题**: `submitJob_Success` 同时验证 repository save、MQ 发送、DTO 转换，原子性不足
- **修改建议**: 拆分为 3 个独立测试：save 验证、MQ 验证、DTO 字段验证

### 2. 【可重复性】ConversationApplicationServiceTest 依赖 MessageProvider 的全局 mock
- **文件**: `ConversationApplicationServiceTest.java`
- **问题**: `@BeforeEach` 中全局 mock `messageProvider.getMessage()`，导致测试间隐式耦合
- **修改建议**: 在每个需要 messageProvider 的测试中显式 stub，避免全局状态污染

### 3. 【可追溯性】大量测试文件缺少 @DisplayName
- **影响文件**: `JobApplicationServiceTest.java`, `ConversationApplicationServiceTest.java`, `AuthControllerTest.java`, `ProfileControllerTest.java` 等
- **问题**: 测试方法名使用 snake_case，但缺少中文/英文 @DisplayName，无法快速定位测试意图
- **修改建议**: 为所有测试方法添加双语 @DisplayName

### 4. 【完整性】VectorApplicationService 零测试覆盖
- **文件**: `VectorApplicationService.java`
- **风险**: 核心业务（向量生成与持久化）无任何测试，维度校验、异常降级路径未验证
- **修改建议**: 补充正常路径、维度不匹配、异常降级、实体类型分支测试

### 5. 【完整性】JobVectorController / ResumeVectorController 零测试覆盖
- **文件**: `JobVectorController.java`, `ResumeVectorController.java`
- **风险**: HTTP 入口无测试，参数校验、权限控制、异常响应未验证
- **修改建议**: 使用 MockMvc 补充 Controller 单元测试

---

## 🟡 中/低严重问题清单（建议修改）

### 6. 【规范性】部分测试混用 JUnit 原生断言与 AssertJ
- **文件**: `JobApplicationServiceTest.java`, `ConversationApplicationServiceTest.java`
- **问题**: 同时存在 `assertEquals` 与 `assertThat`，断言风格不统一
- **建议**: 统一迁移至 AssertJ

### 7. 【有效性】边界值测试缺失
- **影响**: ResumeVersion 状态转换、Job 状态机、分页查询
- **建议**: 补充状态边界（PENDING → COMPLETED → FAILED 转换）、分页边界（page=0, size=MAX）

### 8. 【可执行性】Integration Tests 依赖真实数据库
- **文件**: `AuthIntegrationTest.java`, `ResumeVersionIntegrationTest.java`
- **问题**: 未使用 @DataJpaTest 或内存数据库隔离，可重复性受环境数据影响
- **建议**: 引入 TestContainers 或 H2 内存数据库

### 9. 【可维护性】测试方法过长
- **文件**: `ConversationApplicationServiceTest.java`
- **问题**: `saveAiReply_FirstModification_CreatesAiOptimizedVersion` 等测试超过 30 行，维护困难
- **建议**: 提取 Given 数据构建为私有工厂方法

---

## 📝 缺失场景补充建议（P0 优先级）

### P0 - 必须补充（核心业务无覆盖）

- [ ] **VectorApplicationService**: 正常生成、维度不匹配降级、异常降级、RESUME/JOB 分支
- [ ] **FailedVectorPersistenceService**: 失败向量持久化、重复失败处理
- [ ] **JobVectorSearchService**: 相似度搜索、空结果、异常降级
- [ ] **ResumeVectorBatchService / JobVectorBatchService**: 批量向量生成、部分失败处理
- [ ] **VectorGenerationFacadeAdapter**: 委托调用验证
- [ ] **JobVectorController / ResumeVectorController**: MockMvc 测试（参数绑定、异常处理）
- [ ] **CaptchaController**: 验证码生成、校验、过期处理
- [ ] **MinioFileStorageService**: 上传、下载、删除、预签名 URL 生成
- [ ] **MessageProviderImpl**: 多语言消息解析、缺失 key 降级
- [ ] **ResumeDownloadService**: 文件下载、格式转换、权限校验
- [ ] **ResumeDeletionService**: 软删除、级联删除、权限校验
- [ ] **ResumeVersionChainManager**: 版本链构建、回滚、激活

### P1 - 重要补充（边界与异常）

- [ ] **Job 状态机完整流转**: SCRAPING → PARSING → COMPLETED / FAILED
- [ ] **ResumeGroup 不变式**: 同类型版本唯一 ACTIVE 强制验证
- [ ] **Conversation 消息上限**: 消息数量边界、内容长度边界
- [ ] **分页查询边界**: page < 0, size = 0, size > max
- [ ] **权限交叉场景**: 管理员访问普通用户数据、已删除数据访问

### P2 - 建议补充（非功能与配置）

- [ ] **Configuration Properties 校验**: EmbeddingProperties, MinioConfig, RedisConfig
- [ ] **Scheduler 触发测试**: OutboxCleanupScheduler, OutboxRelayScheduler 定时逻辑
- [ ] **JWT Filter 异常路径**: 过期 token、伪造 token、缺失 token
- [ ] **GlobalExceptionHandler**: 各异常类型的 HTTP 状态码映射

---

## ✅ 评审总结与行动项

| 类别 | 数量 | 行动 |
|:---|:---|:---|
| 高严重问题 | 5 项 | 本轮必须修复 |
| 中低严重问题 | 5 项 | 建议下轮修复 |
| P0 缺失用例 | ~12 个类 | 本轮补充 |
| P1 缺失用例 | ~5 个场景 | 下轮补充 |

**本轮目标**: 高严重问题清零 + P0 缺失类补充测试 → 目标评分 ≥ 80 (B 级)

---

*报告生成时间: 2026-05-25*  
*Skill: testcase-reviewer*
