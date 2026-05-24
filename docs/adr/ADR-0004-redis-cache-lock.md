<!-- Language Switcher / 语言切换 / 語言切換 -->
> [English](ADR-0004-redis-cache-lock.md) | [简体中文](../i18n/zh-Hans-CN/adr/ADR-0004-redis-cache-lock.md) | [繁體中文](../i18n/zh-Hant-TW/adr/ADR-0004-redis-cache-lock.md)

# ADR-0004: Adopt Redis as Distributed Cache and Distributed Lock Infrastructure

| Attribute | Value |
|-----------|-------|
| **Status** | Accepted |
| **Date** | 2025-03 |
| **Deciders** | Backend Architecture Team |
| **Affected Modules** | `backend/infrastructure/cache/`, `backend/infrastructure/lock/`, `backend/app/user/service/` |

---

## 1. Context

ResumeAssistant has the following non-functional requirements that require in-memory high-speed storage:

| Requirement Scenario | Problem Description | Desired Characteristics |
|----------------------|---------------------|-------------------------|
| **Verification Code TTL** | SMS/email verification codes for user registration/login must be valid for 5 minutes | Key-value storage + TTL auto-expiration |
| **Distributed Task Deduplication** | `OutboxRelayScheduler` and `IncrementalRetrainingScheduler` must guarantee single-instance execution in multi-instance microservice deployments | Distributed lock + auto-release |
| **AI Call Throttling Cache** | Repeated submission of the same resume within a short timeframe should avoid duplicate external AI API calls (cost-sensitive) | Short-lived cache + idempotency key |
| **Session State** | WebSocket conversation context requires fast read/write | Low latency, high-concurrency read/write |

### 1.1 Candidate Solutions

| Solution | Description | Evaluation |
|----------|-------------|------------|
| **A. Redis** | Single-threaded event-loop in-memory KV store supporting TTL, Lua scripts, RedLock | Excellent performance, mature ecosystem, first-class Spring Data Redis support |
| **B. Caffeine (Local Cache)** | Pure JVM in-memory cache, fastest speed | Cannot meet distributed scenarios (cache inconsistency and lock invalidation across instances) |
| **C. PostgreSQL Advisory Lock** | Use PG `pg_advisory_lock` for distributed locking | Limited lock granularity, no TTL auto-release mechanism, deadlock risk |
| **D. Etcd / ZooKeeper** | Strongly-consistent distributed coordination service | Too heavy; introduces additional operational burden beyond current complexity needs |

---

## 2. Decision

**Adopt Solution A: Redis as the unified infrastructure for distributed caching and distributed locking.**

### 2.1 Technical Stack Details

| Component | Version | Purpose |
|-----------|---------|---------|
| Redis Server | 7.x | Cache and lock service |
| Spring Data Redis | 3.x | Standard integration within Spring ecosystem |
| ShedLock | 5.x | Distributed scheduled task locking based on Redis |
| Redisson (reserved) | — | To be introduced if RedLock or more complex lock semantics are needed in the future |

### 2.2 Usage Scenario Mapping

#### 2.2.1 Verification Code Cache

```java
// App layer — VerificationCodeService (domain logic: generate, verify, expire)
@Component
public class VerificationCodeService {
    private final CodeStoragePort codeStorage;  // Port interface, implemented by Redis

    public void sendCode(String email) {
        String code = generateCode();
        codeStorage.save(email, code, Duration.ofMinutes(5));  // Redis SETEX 5 minutes
    }

    public boolean verify(String email, String input) {
        String stored = codeStorage.find(email);  // Returns null after expiration
        return stored != null && stored.equals(input);
    }
}
```

```java
// Infrastructure layer — RedisCodeStorageAdapter
@Repository
public class RedisCodeStorageAdapter implements CodeStoragePort {
    private final StringRedisTemplate redis;

    public void save(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }
}
```

#### 2.2.2 Distributed Lock (ShedLock)

```java
// Infrastructure layer — ShedLockConfig
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
// App layer — Any scheduled task requiring single-instance execution
@Scheduled(fixedRate = 5000)
@ShedLock(name = "outbox-relay", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
public void relayOutbox() {
    // In multi-instance deployment, only one instance acquires the lock and executes
}
```

#### 2.2.3 AI Call Cache (Anti-Dup)

```java
// Infrastructure layer — Redis-based idempotency key
@Component
public class AiCallDeduplicationAdapter implements AiCallDeduplicationPort {
    private final StringRedisTemplate redis;

    public boolean tryAcquire(String resumeId, String jobId) {
        String key = "ai:dedup:" + resumeId + ":" + jobId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        return Boolean.TRUE.equals(acquired);  // Only first request returns true; subsequent duplicates rejected
    }
}
```

---

## 3. Consequences

### 3.1 Positive

| Benefit | Description |
|---------|-------------|
| **Development Efficiency** | Spring Data Redis provides `RedisTemplate` / `StringRedisTemplate`; no manual connection pool or serialization logic needed. |
| **Minimal Operations** | Single Docker Compose container deployment; managed uniformly alongside PostgreSQL and RabbitMQ in `docker-compose.yml`. |
| **Auto Lock Release** | ShedLock based on Redis TTL; even if an application instance crashes, the lock auto-releases after `lockAtMostFor`, eliminating deadlock risk. |
| **Single-Instance Deployment Compatible** | Redis in single-instance mode degrades to local cache + local lock with no functional loss; no conditional branch code required. |

### 3.2 Negative

| Cost | Description |
|------|-------------|
| **Cache-Database Consistency** | Cache updates and database writes are non-atomic; extreme race conditions may cause brief inconsistency (eventual consistency accepted). |
| **Redis Single-Point Failure** | Current single-node deployment; failure causes cache penetration and lock invalidation, degrading verification code and throttling functions. |
| **Memory Cost** | Redis is pure in-memory storage; continuous growth of verification codes + session state + idempotency keys requires monitoring memory limits. |
| **Serialization Pitfalls** | Spring Data Redis default JDK serialization has poor compatibility; already configured to `StringRedisSerializer` / `GenericJackson2JsonRedisSerializer`. |

### 3.3 Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Redis memory exhaustion causing OOM | **Memory Limit**: Docker container configured with `--memory=512mb`, paired with `maxmemory-policy=allkeys-lru` for automatic cold data eviction. |
| Cache avalanche (mass requests querying expired key simultaneously) | **Bloom Filter Reserved**: Current data volume is small; not yet introduced. If future hotspot keys expire concurrently, mitigate via mutex locks or randomized TTL scattering. |
| Distributed lock clock drift (ShedLock depends on system time) | **NTP Sync**: Container host enables NTP service; Spring Boot application containers inherit host time. |
| Multi-environment configuration confusion (dev/staging/prod connect to different Redis) | **Externalized Configuration**: `.env` file defines `REDIS_HOST` / `REDIS_PORT`; hardcoded connection addresses forbidden in code. |

---

## 4. Compliance Verification

- **Cache Hit Rate Monitoring**: Extract `keyspace_hits` / `keyspace_misses` via Redis `INFO stats` to calculate hit rate; review caching strategy when below 80%.
- **Lock Competition Visualization**: ShedLock table (or Redis key scan) statistics on lock wait time; alert when > 30s.
- **Architecture Gate**: `code-analyzer` scans the `domain` layer, forbidding direct dependency on `org.springframework.data.redis` packages; all Redis operations must follow the Port/Adapter pattern.
- **Load Test Baseline**: Before each release, execute Redis operation benchmarks (1000 concurrent SET/GET), P99 latency < 5ms.

---

## 5. Related Decisions

- ADR-0001 — Hexagonal Architecture (`CodeStoragePort` / `AiCallDeduplicationPort` and other cache-related Port definitions)
- ADR-0003 — RabbitMQ + Outbox (ShedLock guarantees single-instance Outbox Relay execution)
- ADR-0006 — Docker Compose Deployment (Redis located in `cache-network`, isolated subnet)

---

## 6. Notes

> Redis in this project is **auxiliary infrastructure**; it **does not own core business state**. All persistent data (users, resumes, jobs, conversations) stays in PostgreSQL. Redis cache is allowed to disappear; lock data rebuilds automatically after failure (ShedLock's `lockAtMostFor` already accounts for this).
>
> **Red Line**: Never use Redis as the sole store for irreplaceable business data. A lost verification code is fine — the user requests another. A JWT revocation list would need backup; we avoid that problem entirely by using short-lived tokens + refresh tokens.

---

*End of ADR-0004*