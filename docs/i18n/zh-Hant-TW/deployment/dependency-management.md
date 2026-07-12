# 相依套件管理策略

> [English](../../../deployment/dependency-management.md) | [简体中文](../../zh-Hans-CN/deployment/dependency-management.md) | [繁體中文](dependency-management.md)

本文說明 JobCopilot 如何管理 Dependabot 自動更新，以及 CVE 修復、主版本升級和例行升級的處理方式。

## 策略概覽

| 更新類型 | 來源 | 審查要求 | 自動合併 | 說明 |
| --- | --- | --- | --- | --- |
| 修補版本 | Dependabot | 無 | 是 | 低風險安全與錯誤修復。 |
| 次版本 | Dependabot | 1 人 | 否 | 必須檢查 release notes 的行為變更。 |
| 主版本 | Dependabot | 不適用 | 不適用 | 自動關閉；確有需要時以一般 PR 提交相容性評估。 |
| CVE 緊急修復 | 手動 PR | 2 人 | 否 | 必須附影響分析與回歸計畫。 |

## 為何阻止 Dependabot 主版本升級

主版本可能帶來破壞性 API、傳遞相依衝突、計畫外遷移和跨服務回歸成本。因此 Dependabot 忽略 `semver-major`，自動工作流程會關閉漏出的主版本 PR。

確需主版本升級時，一般 PR 必須包含：明確原因、依上游變更日誌整理的相容清單、既有測試通過證據，以及兩名程式碼擁有者核准。

## Dependabot 設定

設定檔：`.github/dependabot.yml`

- Maven、npm、pip 與 GitHub Actions 分別於週一至週四檢查。
- 所有生態系合計最多開啟 10 個 PR。
- 忽略 `version-update:semver-major`。
- 使用自動 rebase 降低人工維護成本。

## 自動化工作流程

- `block-major-upgrades.yml`：辨識並關閉 Dependabot 主版本 PR。
- `dependabot-auto-merge.yml`：CI 通過後僅自動合併修補版本；次版本仍需人工審查。
- `dependency-check-nightly.yml`：每週日 02:00 UTC 執行 OWASP dependency-check 並上傳報告。

## 相依套件 PR 的 CI 行為

- 每個 Dependabot PR 都執行一般測試、lint 與型別檢查。
- Dependabot PR 略過 Docker 建置與 Qodana。
- OWASP dependency-check 僅在夜間任務執行，避免暫時性誤報阻擋一般 PR。

## CVE 與安全例外

1. 查看夜間 OWASP 報告或 GitHub Security Advisories。
2. 若修補/次版本已有修正，等待 Dependabot 或提交小型手動 PR。
3. 若只能升級主版本，提交相容性評估、CVE ID 與嚴重度，並取得兩人核准。
4. 僅在確認誤報時修改 `backend/owasp-suppressions.xml`，且必須記錄理由與上游依據。

## 職責與指標

- Dependabot：建立修補/次版本 PR，忽略主版本。
- 程式碼擁有者：3 個工作日內審查次版本 PR。
- 安全負責人：檢查夜間報告並推動 CVE 例外。
- 目標：開啟中的 Dependabot PR 少於 5 個；修補版本 24 小時內合併；次版本 3 個工作日內處理；不允許計畫外主版本進入 `main`。
