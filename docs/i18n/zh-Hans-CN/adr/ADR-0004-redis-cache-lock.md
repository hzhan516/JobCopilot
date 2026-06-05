<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0004-redis-cache-lock.md) | [简体中文](ADR-0004-redis-cache-lock.md) | [繁體中文](../../zh-Hant-TW/adr/ADR-0004-redis-cache-lock.md)

# ADR-0004: 采用 Redis 作为分布式缓存与分布式锁基础设施

| 属性 | 内容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 后端架构团队 |
| **Affected Modules** | `backend/infrastructure/cache/`, `backend/infrastructure/lock/`, `backend/app/user/service/` |

---

## 1. Context / 背景

JobCopilot 在运行过程中存在以下非功能性需求，需要内存级高速存储支持：

| 需求场景 | 问题描述 | 期望特性 |
|----------|----------|----------|
| **验证码时效** | 用户注册/登录时的短信/邮件验证码需在 5 分钟内有效 | 键值存储 + TTL 自动过期 |
| **分布式任务防重** | `OutboxRelayScheduler` 与 `IncrementalRetrainingScheduler` 在微服务多实例部署时需保证仅一个实例执行 | 分布式锁 + 自动释放 |
| **AI 调用限流缓存** | 同一简历短时间内重复提交，避免重复调用外部 AI API（成本敏感） | 短暂缓存 + 幂等键 |
| **会话状态** | WebSocket 对话上下文需快速读写 | 低延迟、高并发读写 |

### 1.1 候选方案

| 方案 | 说明 | 评估 |
|------|------|------|
| **A. Redis** | 单线程事件循环的内存 KV 存储，支持 TTL、Lua 脚本、RedLock | 性能优异、生态成熟、Spring Data Redis 一等公民支持 |
| **B. Caffeine（本地缓存）** | 纯 JVM 内缓存，速度最快 | 无法满足分布式场景（多实例间缓存不一致、锁失效） |
| **C. PostgreSQL advisory lock** | 使用 PG 的 `pg_advisory_lock` 实现分布式锁 | 锁粒度受限，无 TTL 自动释放机制，死锁风险 |
| **D. Etcd / ZooKeeper** | 强一致性分布式协调服务 | 过重，引入额外运维负担，超出当前需求复杂度 |

---

## 2. Decision / 决策

**采用方案 A：Redis 作为分布式缓存与分布式锁的唯一基础设施。**

### 2.1 技术选型详情

| 组件 | 版本 | 用途 |
|------|------|------|
| Redis Server | 7.x | 缓存与锁服务 |
| Spring Data Redis | 3.x | Spring 生态标准集成 |
| ShedLock | 5.x | 基于 Redis 的分布式定时任务锁 |
| Redisson（预留） | — | 未来若需 RedLock 或更复杂的锁语义时引入 |

### 2.2 使用场景映射

#### 2.2.1 验证码缓存

```java
// app 层 — VerificationCodeService（领域逻辑：生成、校验、过期）
@Component
public class VerificationCodeService {
    private final CodeStoragePort codeStorage;  // Port 接口，由 Redis 实现

    public void sendCode(String email) {
        String code = generateCode();
        codeStorage.save(email, code, Duration.ofMinutes(5));  // Redis SETEX 5分钟
    }

    public boolean verify(String email, String input) {
        String stored = codeStorage.find(email);  // 过期后自动返回 null
        return stored != null && stored.equals(input);
    }
}
```

```java
// infrastructure 层 — RedisCodeStorageAdapter
@Repository
public class RedisCodeStorageAdapter implements CodeStoragePort {
    private final StringRedisTemplate redis;

    public void save(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }
}
```

#### 2.2.2 分布式锁（ShedLock）

```java
// infrastructure 层 — ShedLockConfig
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory factory) {
        return new RedisLockProvider(factory.getConnection());
    }
}
```

```java
// app 层 — 任何需要单实例执行的定时任务
@Scheduled(fixedRate = 5000)
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
public void relayOutbox() {
    // 多实例部署时，仅一个实例获得锁并执行
}
```

#### 2.2.3 AI 调用缓存（Anti-Dup）

```java
// infrastructure 层 — 基于 Redis 的幂等键
@Component
public class AiCallDeduplicationAdapter implements AiCallDeduplicationPort {
    private final StringRedisTemplate redis;

    public boolean tryAcquire(String resumeId, String jobId) {
        String key = "ai:dedup:" + resumeId + ":" + jobId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        return Boolean.TRUE.equals(acquired);  // 仅首次返回 true，后续重复请求被拒绝
    }
}
```

---

## 3. Consequences / 后果

### 3.1 Positive / 正面

| 收益 | 说明 |
|------|------|
| **开发效率** | Spring Data Redis 提供 `RedisTemplate` / `StringRedisTemplate`，无需手写连接池或序列化逻辑。 |
| **运维极简** | Docker Compose 单容器部署，与 PostgreSQL、RabbitMQ 统一在 `docker-compose.yml` 中管理。 |
| **锁自动释放** | ShedLock 基于 Redis TTL，即使应用实例崩溃，锁也会在 `lockAtMostFor` 后自动释放，无死锁风险。 |
| **单实例部署兼容** | Redis 在单机部署时退化为本地缓存 + 本地锁，功能无损，无需条件分支代码。 |

### 3.2 Negative / 负面

| 成本 | 说明 |
|------|------|
| **缓存与数据库一致性** | 缓存更新与数据库写入非原子操作，极端竞态下可能出现短暂不一致（接受最终一致性）。 |
| **Redis 单点故障** | 当前单节点部署，故障时缓存穿透、锁失效，验证码与限流功能降级。 |
| **内存成本** | Redis 纯内存存储，验证码 + 会话状态 + 幂等键的数据量持续增长需监控内存上限。 |
| **序列化陷阱** | Spring Data Redis 默认 JDK 序列化兼容性差，已配置为 `StringRedisSerializer` / `GenericJackson2JsonRedisSerializer`。 |

### 3.3 Risks / 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Redis 内存耗尽导致 OOM | **内存上限**：Docker 容器设置 `--memory=512mb`，配合 `maxmemory-policy=allkeys-lru` 自动淘汰冷数据。 |
| 缓存击穿（大量请求同时查询已过期 key） | **布隆过滤器预留**：当前数据量小暂不引入，若未来热点 key 集中过期，通过互斥锁或随机 TTL 打散。 |
| 分布式锁时钟漂移（ShedLock 依赖系统时间） | **NTP 同步**：容器宿主启用 NTP 服务，Spring Boot 应用容器继承宿主时间。 |
| 多环境配置混乱（dev/staging/prod 连接不同 Redis） | **配置外置**：`.env` 文件定义 `REDIS_HOST` / `REDIS_PORT`，禁止在代码中硬编码连接地址。 |

---

## 4. Compliance / 合规验证

- **缓存命中率监控**：通过 Redis `INFO stats` 提取 `keyspace_hits` / `keyspace_misses`，计算命中率，低于 80% 时审查缓存策略。
- **锁竞争可视化**：ShedLock 表（或 Redis key 扫描）统计锁等待时间，> 30s 时告警。
- **架构门禁**：`code-analyzer` 扫描 `domain` 层，禁止直接依赖 `org.springframework.data.redis` 包；所有 Redis 操作必须通过 Port/Adapter 模式。
- **压测基线**：每次发布前执行 Redis 操作基准测试（SET/GET 1000 并发），P99 延迟 < 5ms。

---

## 5. Related / 相关决策

- ADR-0001 — 六边形架构（`CodeStoragePort` / `AiCallDeduplicationPort` 等缓存相关 Port 定义）
- ADR-0003 — RabbitMQ + Outbox（ShedLock 保证 Outbox Relay 单实例执行）
- ADR-0006 — Docker Compose 部署（Redis 位于 `cache-network`，独立网段隔离）

---

## 6. Notes / 备注

> Redis 在本项目中是**辅助基础设施**，**不承载核心业务状态**。所有持久化数据（用户、简历、职位、对话记录）仍由 PostgreSQL 管理。Redis 缓存允许丢失；锁数据允许在故障后重建（ShedLock 的 `lockAtMostFor` 已考虑此场景）。
>
> **红线**：绝不使用 Redis 作为不可替代业务数据的唯一存储。验证码丢了无所谓 — 用户再请求一次。JWT 撤销列表若放在 Redis 则需备份；我们直接避免这个问题：短时效 token + refresh token。

---

*End of ADR-0004*
