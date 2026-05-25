# @Transactional 統一策略文件

> **語言**: [English](../../transactional-strategy.md) | [簡體中文](../zh-Hans-CN/transactional-strategy.md) | **繁體中文**

> 本文件規定 ResumeAssistant 後端 `application` 模組中 Spring `@Transactional` 的使用規範。
> 目標：消除交易邊界洩漏、巢狀交易污染、長交易阻塞等隱患。
>
> 版本：2026-05-25 | 分支：`sanitize-for-oss`

---

## 1. 核心原則

| # | 原則 | 理由 |
|---|---|---|
| 1 | **指令用 `@Transactional`，查詢用 `@Transactional(readOnly = true)`** | 利用唯讀交易最佳化（Hibernate 不髒檢查、不 flush），減少 DB 寫入鎖。 |
| 2 | **類級唯讀，方法級覆寫** | 讀多寫少的 Service，在類別上宣告 `@Transactional(readOnly = true)`，寫入方法單獨標記 `@Transactional`。減少重複註解。 |
| 3 | **禁止在交易內做網路 I/O（HTTP、MQ、外部檔案）** | 長交易 = 長連線佔用，降低吞吐量；外部失敗導致交易回滾，與業務語意不符。 |
| 4 | **跨聚合寫入作業各自獨立交易** | 不同聚合根（Job、Resume、Conversation…）的寫入作業不應共用交易；用 `Propagation.REQUIRES_NEW` 或拆分至獨立 Service。 |
| 5 | **Scheduler / 非同步任務必須獨立交易邊界** | 定時任務執行週期長，預設交易應最小化；涉及 Outbox 模式時，relay 與 cleanup 分交易。 |
| 6 | **所有 `@Transactional` 必須顯式宣告 `timeout`** | 防止慢查詢、鎖等待或意外網路 I/O 導致無限長交易占用連線池。純 DB 操作 30s；含 batch/sync 的 60s。 |
| 7 | **聚合根必須有樂觀鎖（`@Version`）** | 防止併發寫場景下的遺失更新。當前已應用到 `Job`、`ResumeGroup`、`Conversation`、`JobMatchResult`、`ApplicationTracking`、`User`。 |

---

## 2. 註解矩陣

| 場景 | 推薦註解 | 目前狀態 |
|---|---|---|
| **純查詢**（get/list/find/search） | 類級 `@Transactional(readOnly = true)` + 無額外方法註解 | ✅ 已統一 |
| **單聚合根寫入**（save/update/delete） | 方法級 `@Transactional` | ✅ 已統一 |
| **批次寫入**（batchUpsert） | 方法級 `@Transactional` | ✅ 已統一 |
| **寫入後需發 MQ / HTTP** | **交易內只寫 DB**，發 MQ 在交易提交後（交易監聽器、ApplicationEvent、或 Outbox） | ⚠️ 見 §3.1 |
| **失敗日誌持久化**（不干擾主交易） | `@Transactional(propagation = REQUIRES_NEW, timeout = 30)` | ✅ 已統一 |
| **Scheduler（Outbox relay）** | 讀取 `@Transactional(readOnly = true, timeout = 30)` + 發送後更新狀態用 `REQUIRES_NEW, timeout = 30` | ⚠️ 見 §3.2 |
| **Scheduler（cleanup）** | `@Transactional(timeout = 30)`（短交易，批次刪除） | ✅ 已統一 |

---

## 3. 目前已知問題

### 3.1 [已修復] 交易內發送 MQ —— `JobSubmissionService.submit()`

**檔案**：`app/.../JobSubmissionService.java:31`

```java
@Transactional
public JobResponse submit(UUID userId, SubmitJobRequest request) {
    ...
    jobRepository.save(job);          // ← DB 寫入
    aiMessagePublisherPort.sendJobForParsing(...);  // ← 網路 I/O（RabbitMQ / HTTP）
    ...
}
```

**風險**：
1. MQ 發送失敗 → 整個交易回滾 → 已 save 的 Job 遺失，但使用者已收到提交成功回應。
2. MQ 發送成功但交易回滾 → 訊息已發出但 DB 無記錄 → 消費者找不到 Job。

**狀態**：✅ **已修復** — 無需程式碼修改。`AiMessagePublisherPort` 實際已採用 **Outbox 模式**（交易內僅寫入 `outbox` 表；`OutboxRelayScheduler` 輪詢後發送至 RabbitMQ）。稽核期間確認。

---

### 3.2 [已修復] Scheduler 交易邊界模糊

**檔案**：`app/.../OutboxRelayScheduler.java:38`

```java
@Transactional
public void relayPendingMessages() { ... }
```

**風險**：整個 relay 過程在一個交易內：讀取 pending → 逐個發送 → 更新狀態。如果訊息量大，交易極長。

**狀態**：✅ **已修復**，commit `618757f6`。

**已套用的修復**：
- 讀取 pending 用 `@Transactional(readOnly = true)`。
- 提取 `OutboxRelayTransactionService`，使用 `@Transactional(propagation = Propagation.REQUIRES_NEW)`；每條訊息的 RabbitMQ `convertAndSend` + `outboxMessageRepository.save` 執行於獨立交易中，防止單筆失敗或網路阻塞導致整批回滾。

---

### 3.3 [已修復] `JobApplicationService.submitJob()` 與 `JobSubmissionService.submit()` 雙層 `@Transactional` 冗餘

**檔案**：
- `JobApplicationService.java:53`（`@Transactional`）
- `JobSubmissionService.java:31`（`@Transactional`）

`JobApplicationService.submitJob()` 呼叫 `JobSubmissionService.submit()`，兩者都有 `@Transactional`。Spring 預設 `REQUIRED`，內層會加入外層交易。雖然功能正常，但**架構上冗餘**：交易邊界應在最外層統一宣告，內層純業務邏輯不應重複註解。

**狀態**：✅ **已修復**，commit `618757f6`。

**已套用的修復**：去掉 `JobSubmissionService.submit()` 的 `@Transactional`，交易由 `JobApplicationService.submitJob()` 統一控制。
---

### 3.4 `MatchingApplicationService.startJobMatch()` 含向量產生

**檔案**：`MatchingApplicationService.java:58`

**狀態**：✅ **已修復**

**問題**：`startJobMatch()` 持有 `@Transactional`，內部在向量遺失時同步呼叫 `vectorGenerationPort.generateAndSaveVector()`，該連接埠最終透過 HTTP 呼叫 Embedding 服務。長 HTTP 呼叫阻塞資料庫交易，且失敗時會導致整個配對請求回滾。

**修復方案**：
- 提取 `MatchTransactionService`（套件級 Bean），僅封裝 recall + persist + outbox-write 等純 DB 作業，宣告 `@Transactional`。
- `MatchingApplicationService.startJobMatch()` 去掉 `@Transactional`，負責：
  1. 查詢 model version、resume vector（交易外）。
  2. 向量遺失時，在交易外觸發 on-demand 向量產生（HTTP 呼叫）。
  3. 向量就緒後，委派 `matchTransactionService.execute()` 進入短交易完成 recall 與 ranking 請求投遞。
- 交易邊界：HTTP 呼叫（向量產生）在交易外；輕量 DB 作業在交易內。

**修改檔案**：
- `MatchingApplicationService.java`：拆分為 `MatchTransactionService` + `MatchingApplicationService`
- `MatchingApplicationServiceTest.java`：更新測試以適配拆分後的結構

---

### 3.5 [已修復] `JobApplicationService.handleJobProcessResult()` — 交易內向量產生 HTTP 呼叫

**檔案**：`app/.../JobApplicationService.java`

```java
@Transactional
public void handleJobProcessResult(AiResultEvent event) {
    ...
    vectorGenerationService.generateForJob(...);  // ← HTTP 呼叫（Embedding 服務）
    jobDatasetSyncService.sync(job, event);       // ← DB 寫入
    jobRepository.save(job);
}
```

**風險**：`vectorGenerationService.generateForJob()` 觸發對 Embedding 服務的 HTTP 呼叫。若 Embedding 服務回應慢或逾時，資料庫連線將在整個期間被占用，耗盡連線池。

**狀態**：✅ **已修復**。

**已套用的修復**：
- 提取 `JobResultTransactionService`（套件級 Bean），僅封裝純 DB 作業（`markCompleted`/`markFailed` + `sync` + `save`），宣告 `@Transactional(timeout = 60)`。
- `JobApplicationService.handleJobProcessResult()` 去掉 `@Transactional`。短資料庫交易完成後，向量產生在**交易外**執行。
- `JobApplicationService` 及 `application` 模組中所有剩餘的 `@Transactional` 方法現已攜帶顯式 `timeout` 值（純 DB 寫入 30s，batch/sync 作業 60s）。

**修改檔案**：
- `JobApplicationService.java`：重構 `handleJobProcessResult()`；為所有 `@Transactional` 添加 `timeout`
- `JobResultTransactionService.java`：新增檔案（套件級可見）

---

## 4. 最佳實踐速查

```java
/**
 * 讀多寫少的 Service：類級唯讀，寫入方法覆寫。
 */
@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    public JobResponse getJob(...) { ... }          // 繼承 readOnly

    @Transactional
    public JobResponse submitJob(...) { ... }      // 覆寫為讀寫
}

/**
 * 聚合根樂觀鎖範例。
 */
@Version
@Column(name = "version", nullable = false)
private Long version;

public void markCompleted(ParsedJobContent content) {
    if (this.status != JobStatus.PARSING) {
        throw new IllegalStateException("Job must be PARSING to complete.");
    }
    this.status = JobStatus.COMPLETED;
    this.parsedContent = content;
}

/**
 * 純指令 Service：類級不寫 readOnly。
 */
@Service
@RequiredArgsConstructor
public class JobSubmissionService {
    // 不宣告類級 @Transactional，由呼叫方控制交易
}

/**
 * 獨立交易（失敗日誌）。
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveFailedVector(...) { ... }
```

---

## 5. 檢查清單（Code Review 用）

- [ ] 查詢方法是否誤用 `@Transactional`（未加 `readOnly = true`）？
- [ ] 寫入方法內部是否有 HTTP / MQ / 檔案 I/O？若有，是否移到交易外？
- [ ] 是否有 `@Transactional` 巢狀冗餘（兩層 Service 都宣告）？
- [ ] Scheduler / 非同步任務交易是否最小化？
- [ ] 是否出現 `REQUIRES_NEW` 濫用（非日誌/補償類場景）？
- [ ] 每個 `@Transactional` 是否都宣告了顯式 `timeout`（不依賴預設值）？
- [ ] 每個聚合根實體是否都有 `@Version` 樂觀鎖？

---

## 6. 附錄：目前 `@Transactional` 全量清單

### 6.1 類級註解

| 類別 | 註解 | 說明 |
|---|---|---|
| `ConversationApplicationService` | `@Transactional(readOnly = true)` | 類級唯讀，個別寫入方法覆寫 |
| `ConversationQueryService` | `@Transactional(readOnly = true)` | 純查詢 |
| `AuthApplicationService` | `@Transactional(readOnly = true)` | 類級唯讀，register/login 覆寫 |
| `ProfileApplicationService` | `@Transactional(readOnly = true)` | 類級唯讀，update 覆寫 |
| `ModelManagementApplicationService` | — | 兩個 get 用 readOnly，switch 用讀寫 |

### 6.2 寫入作業（`@Transactional` 無 readOnly）

| 類別 | 方法 | 備註 |
|---|---|---|
| `ResumeUploadHandler.upload` | 上傳履歷 | |
| `ResumeApplicationService.handleUpload` | 處理上傳 | |
| `ResumeApplicationService.handleEdit` | 編輯 | |
| `ResumeApplicationService.handleCreateVersion` | 建立版本 | |
| `ResumeApplicationService.handleDelete` | 刪除群組 | |
| `ResumeApplicationService.handleDeleteVersion` | 刪除版本 | |
| `ResumeApplicationService.handleActivateVersion` | 啟用版本 | |
| `ResumeApplicationService.handleParseResult` | 解析結果 | |
| `ResumeVersionChainManager.createVersion` | 版本鏈 | |
| `ResumeDeletionService.deleteGroup` | 刪除群組 | |
| `ResumeDeletionService.deleteVersion` | 刪除版本 | |
| `ResumeParseResultHandler.handle` | 解析結果 | |
| `FailedVectorPersistenceService.saveFailedVector` | `REQUIRES_NEW` | ✅ 正確使用 |
| `JobVectorBatchService.batchUpsert` | 批次 upsert | |
| `ResumeVectorBatchService.batchUpsert` | 批次 upsert | |
| `ConversationApplicationService.createConversation` | 建立對話 | 覆寫類級 readOnly |
| `ConversationApplicationService.sendMessage` | 發訊息 | |
| `ConversationApplicationService.saveAiReply` | 儲存 AI 回覆 | 兩個多載 |
| `ConversationApplicationService.uploadAttachment` | 上傳附件 | 含檔案流 |
| `ConversationApplicationService.closeConversation` | 關閉對話 | |
| `ConversationApplicationService.deleteConversation` | 刪除對話 | |
| `AuthApplicationService.registerByEmail` | 註冊 | |
| `AuthApplicationService.loginByGoogle` | Google 登入 | |
| `ProfileApplicationService.updateProfile` | 更新資料 | |
| `ProfileApplicationService.updateAvatar` | 更新頭像 | |
| `OutboxCleanupScheduler.cleanup` | 清理 Outbox | |
| `OutboxRelayScheduler.relayPendingMessages` | ⚠️ 長交易 | |
| `ModelManagementApplicationService.switchActiveModel` | 切換模型 | |
| `MatchingApplicationService.startJobMatch` | ⚠️ 需確認含外部呼叫？ | |
| `MatchingApplicationService.saveMatchResult` | 儲存結果 | |
| `TrackingApplicationService.createTracking` | 建立追蹤 | |
| `TrackingApplicationService.updateTracking` | 更新追蹤 | |
| `TrackingApplicationService.deleteTracking` | 刪除追蹤 | |
| `JobScoringContextLoader.load` | 載入評分上下文 | |
| `JobScoringResultSaver.save` | 儲存評分結果 | |
| `JobSubmissionService.submit` | ⚠️ 交易內發 MQ | 見 §3.1 |
| `JobApplicationService.submitJob` | 提交職位 | |
| `JobApplicationService.handleJobProcessResult` | 處理結果 | |
| `JobApplicationService.updateJob` | 更新職位 | |
| `JobApplicationService.deleteJob` | 刪除職位 | |
| `JobApplicationService.trackUserAction` | 追蹤行為 | |

### 6.3 唯讀作業（`@Transactional(readOnly = true)`）

| 類別 | 方法 |
|---|---|
| `ResumeApplicationService.getGroup` | |
| `ResumeApplicationService.listUserGroups` | |
| `ResumeApplicationService.getVersion` | |
| `ConversationApplicationService` | （類級） |
| `ConversationQueryService` | （類級） |
| `AuthApplicationService` | （類級，register/login 覆寫） |
| `ProfileApplicationService` | （類級，update 覆寫） |
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

## 7. 修訂歷史

| 日期 | 修訂人 | 內容 |
|---|---|---|
| 2026-05-25 | AI Architecture Audit | 為 `Job` 聚合根添加 `@Version` 樂觀鎖（§1.7、§6.4）。修復 `handleJobProcessResult` 交易內 HTTP 呼叫問題（§3.5）。所有 `@Transactional` 方法統一顯式 `timeout`。 |
