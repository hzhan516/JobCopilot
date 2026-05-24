## 变更内容
<!-- 这个 PR 做了什么？一句话概括。 -->

## 原因
<!-- 为什么需要这个变更？链接到 Issue 或解释问题。 -->

## 实现方式
<!-- 技术方案和关键决策。 -->

## 测试
<!-- 如何测试的？ -->
- [ ] 单元测试通过 (`cd backend && mvn test`)
- [ ] 架构测试通过 (`cd backend && mvn test -Dtest="*ArchitectureTest*"`)
- [ ] 集成测试通过 (`cd backend && mvn verify -Pintegration-test`)
- [ ] 前端 lint 通过 (`cd frontend && npm run lint`)
- [ ] AI 服务 lint 通过 (`cd ai-service && ruff check .`)
- [ ] 已进行手工测试
- [ ] 边界情况已覆盖

## 截图
<!-- UI 变更时提供 -->

## 检查清单
- [ ] 已自我审查代码
- [ ] 已添加/更新测试
- [ ] 已更新文档（如适用，需提供英语 + 简体中文 + 繁体中文）
- [ ] 无新增警告
- [ ] 破坏性变更已记录
- [ ] 包含迁移指南（如有破坏性变更）
- [ ] 遵循六边形架构规则（domain/app 层无框架代码）
