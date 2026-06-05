<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](../../../adr/ADR-0004-redis-cache-lock.md) | [简体中文](../../zh-Hans-CN/adr/ADR-0004-redis-cache-lock.md) | [繁體中文](ADR-0004-redis-cache-lock.md)

# ADR-0004: 採用 Redis 作為分布式缓存與分布式鎖基礎設施

| 屬性 | 內容 |
|------|------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | 後端架構團隊 |
| **Affected Modules** | `backend/infrastructure/cache/`, `backend/infrastructure/lock/`, `backend/app/user/service/` |

---

## 1. Context / 背景

JobCopilot 在運行過程中存在以下非功能性需求，需要記憶體級高速存儲支持：

| 需求場景 | 問題描述 | 期望特性 |
|----------|----------|----------|
| **驗證碼時效** | 用戶注册/登錄時的短信/郵件驗證碼需在 5 分钟內有效 | 键值存儲 + TTL 自動過期 |
| **分布式任務防重** | `OutboxRelayScheduler` 與 `IncrementalRetrainingScheduler` 在微服務多实例部署時需保證仅一个实例執行 | 分布式鎖 + 自動釋放 |
| **AI 呼叫限流缓存** | 同一簡歷短時間內重復提交，避免重復呼叫外部 AI API（成本敏感） | 短暫缓存 + 幂等键 |
| **會話狀态** | WebSocket 對話上下文需快速讀寫 | 低延迟、高並發讀寫 |

### 1.1 候選方案

| 方案 | 說明 | 評估 |
|------|------|------|
| **A. Redis** | 單執行緒事件迴圈的記憶體 KV 存儲，支持 TTL、Lua 脚本、RedLock | 性能優異、生态成熟、Spring Data Redis 一等公民支持 |
| **B. Caffeine（本地缓存）** | 純 JVM 內缓存，速度最快 | 無法滿足分布式場景（多实例間缓存不一致、鎖失效） |
| **C. PostgreSQL advisory lock** | 使用 PG 的 `pg_advisory_lock` 實現分布式鎖 | 鎖粒度受限，無 TTL 自動釋放機制，死鎖風險 |
| **D. Etcd / ZooKeeper** | 強一致性分布式協調服務 | 過重，引入額外運維負擔，超出當前需求複雜度 |

---

## 2. Decision / 決策

**採用方案 A：Redis 作為分布式缓存與分布式鎖的唯一基礎設施。**

### 2.1 技術選型详情

| 組件 | 版本 | 用途 |
|------|------|------|
| Redis Server | 7.x | 缓存與鎖服務 |
| Spring Data Redis | 3.x | Spring 生态標準整合 |
| ShedLock | 5.x | 基於 Redis 的分布式定時任務鎖 |
| Redisson（預留） | — | 未來若需 RedLock 或更複雜的鎖語義時引入 |

### 2.2 使用場景映射

#### 2.2.1 驗證碼缓存

```java
// app 層 — VerificationCodeService（領域逻辑：生成、校驗、過期）
@Component
public class VerificationCodeService {
    private final CodeStoragePort codeStorage;  // Port 介面，由 Redis 實現

    public void sendCode(String email) {
        String code = generateCode();
        codeStorage.save(email, code, Duration.ofMinutes(5));  // Redis SETEX 5分钟
    }

    public boolean verify(String email, String input) {
        String stored = codeStorage.find(email);  // 過期後自動返回 null
        return stored != null && stored.equals(input);
    }
}
```

```java
// infrastructure 層 — RedisCodeStorageAdapter
@Repository
public class RedisCodeStorageAdapter implements CodeStoragePort {
    private final StringRedisTemplate redis;

    public void save(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }
}
```

#### 2.2.2 分布式鎖（ShedLock）

```java
// infrastructure 層 — ShedLockConfig
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
// app 層 — 任何需要單实例執行的定時任務
@Scheduled(fixedRate = 5000)
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
public void relayOutbox() {
    // 多实例部署時，仅一个实例获得鎖並執行
}
```

#### 2.2.3 AI 呼叫缓存（Anti-Dup）

```java
// infrastructure 層 — 基於 Redis 的幂等键
@Component
public class AiCallDeduplicationAdapter implements AiCallDeduplicationPort {
    private final StringRedisTemplate redis;

    public boolean tryAcquire(String resumeId, String jobId) {
        String key = "ai:dedup:" + resumeId + ":" + jobId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        return Boolean.TRUE.equals(acquired);  // 仅首次返回 true，後續重復请求被拒绝
    }
}
```

---

## 3. Consequences / 後果

### 3.1 Positive / 正面

| 收益 | 說明 |
|------|------|
| **開發效率** | Spring Data Redis 提供 `RedisTemplate` / `StringRedisTemplate`，無需手寫連接池或序列化逻辑。 |
| **運維極簡** | Docker Compose 單容器部署，與 PostgreSQL、RabbitMQ 統一在 `docker-compose.yml` 中管理。 |
| **鎖自動釋放** | ShedLock 基於 Redis TTL，即使應用实例崩溃，鎖也會在 `lockAtMostFor` 後自動釋放，無死鎖風險。 |
| **單实例部署兼容** | Redis 在單機部署時退化為本地缓存 + 本地鎖，功能無損，無需條件分支程式碼。 |

### 3.2 Negative / 負面

| 成本 | 說明 |
|------|------|
| **缓存與資料庫一致性** | 缓存更新與資料庫寫入非原子操作，極端競态下可能出現短暫不一致（接受最終一致性）。 |
| **Redis 單點故障** | 當前單節點部署，故障時缓存穿透、鎖失效，驗證碼與限流功能降級。 |
| **記憶體成本** | Redis 純記憶體存儲，驗證碼 + 會話狀态 + 幂等键的資料量持續增長需监控記憶體上限。 |
| **序列化陷阱** | Spring Data Redis 預設 JDK 序列化兼容性差，已設定為 `StringRedisSerializer` / `GenericJackson2JsonRedisSerializer`。 |

### 3.3 Risks / 風險與缓解

| 風險 | 缓解措施 |
|------|----------|
| Redis 記憶體耗盡導致 OOM | **記憶體上限**：Docker 容器設置 `--memory=512mb`，配合 `maxmemory-policy=allkeys-lru` 自動淘汰冷資料。 |
| 缓存击穿（大量请求同時查询已過期 key） | **布隆過濾器預留**：當前資料量小暫不引入，若未來熱點 key 集中過期，通過互斥鎖或隨機 TTL 打散。 |
| 分布式鎖時钟漂移（ShedLock 依賴係統時間） | **NTP 同步**：容器宿主啟用 NTP 服務，Spring Boot 應用容器繼承宿主時間。 |
| 多环境設定混乱（dev/staging/prod 連接不同 Redis） | **設定外置**：`.env` 檔案定義 `REDIS_HOST` / `REDIS_PORT`，禁止在程式碼中硬编碼連接地址。 |

---

## 4. Compliance / 合規驗證

- **缓存命中率监控**：通過 Redis `INFO stats` 提取 `keyspace_hits` / `keyspace_misses`，计算命中率，低於 80% 時審查缓存策略。
- **鎖競争可視化**：ShedLock 表（或 Redis key 掃描）統计鎖等待時間，> 30s 時告警。
- **架構门禁**：`code-analyzer` 掃描 `domain` 層，禁止直接依賴 `org.springframework.data.redis` 包；所有 Redis 操作必須通過 Port/Adapter 模式。
- **壓測基線**：每次發布前執行 Redis 操作基準測試（SET/GET 1000 並發），P99 延迟 < 5ms。

---

## 5. Related / 相關決策

- ADR-0001 — 六边形架構（`CodeStoragePort` / `AiCallDeduplicationPort` 等缓存相關 Port 定義）
- ADR-0003 — RabbitMQ + Outbox（ShedLock 保證 Outbox Relay 單实例執行）
- ADR-0006 — Docker Compose 部署（Redis 位於 `cache-network`，獨立網段隔離）

---

## 6. Notes / 備注

> Redis 在本項目中擔任**輔助基礎設施**，**不承載核心業務狀态**。所有持久化資料（用戶、簡歷、职位、對話記錄）仍由 PostgreSQL 管理。Redis 緩存允許丟失；鎖資料允許在故障後重建（ShedLock 的 `lockAtMostFor` 已考慮此場景）。
>
> **紅線**：絕不使用 Redis 作為不可替代業務資料的唯一存儲。驗證碼丟了無所謂 — 用戶再請求一次。JWT 撤銷列表若放在 Redis 則需備份；我們直接避免這個問題：短時效 token + refresh token。

---

*End of ADR-0004*
