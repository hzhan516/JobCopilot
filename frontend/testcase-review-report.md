# 前端测试用例评审报告
# Frontend Test Case Review Report

> 评审日期: 2026-05-25
> 评审人: testcase-reviewer Skill (AI-assisted)
> 框架: Vitest + @testing-library/react + happy-dom

---

## 一、评审范围

| 维度 | 数据 |
|------|------|
| 生产文件总数 | 109 个 (.ts/.tsx) |
| 测试文件总数 | 12 个 (.test.ts/.test.tsx) |
| 测试方法总数 | ~70 个 |
| 文件级覆盖率 | ~11% |

## 二、分层覆盖率

| 层级 | 生产文件数 | 测试文件数 | 覆盖率 |
|------|-----------|-----------|--------|
| components | 69 | 2 (button, ParseStatusBadge) | 2.9% |
| hooks | 5 | 1 (useAuth) | 20% |
| services | 8 | 5 (job, resume, tokenStorage, tracking, api mock间接) | 62.5% |
| store | 4 | 2 (job, language) | 50% |
| utils | 2 | 2 (file, i18n) | 100% |
| lib | 1 | 1 (utils/cn) | 100% |
| pages | 18 | 0 | 0% |

## 三、评分（六维模型）

| 维度 | 评分 | 权重 | 加权分 |
|------|------|------|--------|
| 文件覆盖率 | 20/100 | 20% | 4 |
| 核心业务覆盖 | 35/100 | 25% | 8.75 |
| 原子性 | 75/100 | 15% | 11.25 |
| 独立性 | 70/100 | 15% | 10.5 |
| 可重复性 | 85/100 | 10% | 8.5 |
| 可追溯性 | 60/100 | 15% | 9 |
| **综合评分** | — | — | **52/100 (D 级)** |

## 四、高严重问题（P0）

### 1. api.ts 拦截器零测试 — 核心业务逻辑缺失
- **影响**: Token 自动续期、请求重试、JWT 附加、语言头附加等核心网络层逻辑无任何测试
- **风险**: 401 刷新死循环、指数退避重试、并发刷新队列等复杂逻辑无回归保障

### 2. resume.store.ts / profile.store.ts 零测试
- **影响**: Zustand 状态管理中的数据适配（adaptGroup/adaptVersion）、轮询解析状态、版本链操作等无测试
- **风险**: 前端数据模型与后端 API 不匹配时，问题延迟到运行时才发现

### 3. useAbortableRequest / use-mobile / useTimeZone 零测试
- **影响**: 组件卸载时自动取消请求、移动端响应式适配、时区切换同步等关键 UX 逻辑无测试
- **风险**: 内存泄漏（未取消的请求在组件卸载后更新状态）、响应式布局错误

### 4. ProtectedRoute / PublicRoute / ErrorBoundary 零测试
- **影响**: 路由守卫和全局错误边界无测试
- **风险**: 路由跳转循环、未登录用户访问受保护页面、渲染错误导致白屏

### 5. chatService.ts / profileService.ts 零测试
- **影响**: 聊天服务（含 SSE 流式读取）、用户资料服务无测试
- **风险**: 流式响应处理失败、资料更新后状态不同步

## 五、中低严重问题（P1/P2）

### P1
1. `jobService.test.ts` 仅 3 个测试，缺少 `getJobs`、`scoreJob` 等核心方法测试
2. `resumeService.test.ts` 缺少 `createVersion`、`activateVersion`、`getVersionsByGroup` 测试
3. `job.store.test.ts` 未验证 toast 调用（mock 了 sonner 但未断言）
4. `useAuth.test.tsx` 未覆盖登录失败、注册失败、token 过期自动刷新等异常场景

### P2
1. `i18n.test.ts` 中 `getLocale` 依赖全局 i18n.language，测试间可能互相影响
2. 缺少覆盖率阈值配置（coverage 配置存在但未设置 threshold）
3. 组件层测试严重不足（仅 2/69 组件有测试）

## 六、P0 缺失清单（按优先级排序）

| 优先级 | 文件 | 测试文件 | 核心场景 |
|--------|------|---------|---------|
| P0 | `src/services/api.ts` | `api.test.ts` | Token 续期、GET 重试、JWT 附加、语言头、createAbortableRequest |
| P0 | `src/store/resume.store.ts` | `resume.store.test.ts` | fetchGroups、fetchGroupDetail、uploadResume、pollParseStatus、saveVersion、deleteGroup |
| P0 | `src/store/profile.store.ts` | `profile.store.test.ts` | fetchProfile、updateProfile、updateAvatar、错误处理 |
| P0 | `src/services/chatService.ts` | `chatService.test.ts` | createConversation、streamAIResponse（SSE） |
| P0 | `src/services/profileService.ts` | `profileService.test.ts` | getProfile、updateProfile、updateAvatar |
| P0 | `src/hooks/useAbortableRequest.ts` | `useAbortableRequest.test.ts` | execute 取消前请求、abort 手动取消、组件卸载自动取消 |
| P0 | `src/hooks/use-mobile.ts` | `use-mobile.test.ts` | 初始状态、matchMedia 变化响应 |
| P0 | `src/hooks/useTimeZone.ts` | `useTimeZone.test.ts` | 初始时区、updateTimeZone、resetTimeZone、storage 事件同步 |
| P0 | `src/components/ProtectedRoute.tsx` | `ProtectedRoute.test.tsx` | 未认证跳转、已认证放行、保存 from 路径 |
| P0 | `src/components/PublicRoute.tsx` | `PublicRoute.test.tsx` | 已认证跳转 resumes、保留 from 路径、未认证放行 |
| P0 | `src/components/layout/ErrorBoundary.tsx` | `ErrorBoundary.test.tsx` | 错误捕获、fallback 渲染、重置、自定义 fallback |

## 七、改进建议

1. **立即补充 P0 测试**（本轮目标）
2. **后续补充 P1 测试**: jobService 扩展、resumeService 扩展、useAuth 异常场景
3. **组件层测试**: 优先覆盖表单交互组件（ResumeUpload、Login、Register）
4. **覆盖率阈值**: 在 `vitest.config.ts` 中配置 `coverage.thresholds` 阻止回归
5. **Store 测试规范**: 统一使用 `useStore.setState(initialState)` 重置，避免测试间状态污染

## 八、结论

前端当前测试覆盖率为 **52/100 (D 级)**，远低于后端（78/100）。核心网络层（api.ts）、状态管理层（resume/profile store）、路由守卫层完全无测试，是最大风险点。建议优先完成 P0 清单，预计可将评分提升至 **70+/100 (C 级)**。
