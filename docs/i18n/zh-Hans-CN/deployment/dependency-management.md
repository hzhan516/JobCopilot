# 依赖管理策略

> [English](../../../deployment/dependency-management.md) | [简体中文](dependency-management.md) | [繁體中文](../../zh-Hant-TW/deployment/dependency-management.md)

本文说明 JobCopilot 如何管理 Dependabot 自动更新，以及 CVE 修复、主版本升级和常规升级的处理方式。

## 策略概览

| 更新类型 | 来源 | 评审要求 | 自动合并 | 说明 |
| --- | --- | --- | --- | --- |
| 补丁版本 | Dependabot | 无 | 是 | 低风险安全与缺陷修复。 |
| 次版本 | Dependabot | 1 人 | 否 | 必须检查 release notes 中的行为变化。 |
| 主版本 | Dependabot | 不适用 | 不适用 | 自动关闭；确有需要时通过普通 PR 提交兼容性评估。 |
| CVE 紧急修复 | 手工 PR | 2 人 | 否 | 必须附影响分析和回归计划。 |

## 为什么阻止 Dependabot 主版本升级

主版本可能带来破坏性 API、传递依赖冲突、计划外迁移和跨服务回归成本。因此 Dependabot 忽略 `semver-major`，自动工作流会关闭漏出的主版本 PR。

确需主版本升级时，普通 PR 必须包含：明确原因、基于上游变更日志的兼容清单、现有测试通过证据，以及两名代码所有者批准。

## Dependabot 配置

配置文件：`.github/dependabot.yml`

- Maven、npm、pip 和 GitHub Actions 分别在周一至周四检查。
- 所有生态系统合计最多打开 10 个 PR。
- 忽略 `version-update:semver-major`。
- 使用自动 rebase 降低人工维护成本。

## 自动化工作流

- `block-major-upgrades.yml`：识别并关闭 Dependabot 主版本 PR。
- `dependabot-auto-merge.yml`：CI 通过后仅自动合并补丁版本；次版本仍需人工评审。
- `dependency-check-nightly.yml`：每周日 02:00 UTC 执行 OWASP dependency-check 并上传报告。

## 依赖 PR 的 CI 行为

- 每个 Dependabot PR 都执行常规测试、lint 和类型检查。
- Dependabot PR 跳过 Docker 构建和 Qodana。
- OWASP dependency-check 只在夜间任务执行，避免短暂误报阻断普通 PR。

## CVE 与安全例外

1. 查看夜间 OWASP 报告或 GitHub Security Advisories。
2. 若补丁/次版本已修复，等待 Dependabot 或提交小型手工 PR。
3. 若只能升级主版本，提交兼容性评估、CVE ID 和严重度，并取得两人批准。
4. 只有确认误报时才能修改 `backend/owasp-suppressions.xml`，且必须记录理由和上游依据。

## 职责与指标

- Dependabot：创建补丁/次版本 PR，忽略主版本。
- 代码所有者：3 个工作日内评审次版本 PR。
- 安全负责人：检查夜间报告并推动 CVE 例外。
- 目标：开放 Dependabot PR 少于 5 个；补丁 24 小时内合并；次版本 3 个工作日内处理；不允许计划外主版本进入 `main`。
