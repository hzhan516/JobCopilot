# JobCopilot 前端布局严重错乱问题定位与修复计划

- 日期：2026-07-12
- 当前分支：`post_review_july`
- 文档范围：问题定位、修复计划、首次修复报告、Codex 复审与剩余问题修复报告
- 用户复现环境：本地 Docker，桌面端，进入 My Resumes / Jobs 后页面错乱
- 截图证据：左侧主导航正常，Master–Detail 列表栏被压缩到约 35–40px，文本逐字换行，并出现内层横向和纵向滚动条；详情栏占据几乎全部剩余宽度

## 1. 结论摘要

本次严重错乱的直接根因已经确认：项目使用 `react-resizable-panels@4.12.1`，该版本将 `Panel` 的数值型 `defaultSize`、`minSize`、`maxSize` 解释为 **像素（px）**；但 My Resumes、Jobs、Applications 三个 Master–Detail 页面仍把 `35`、`40`、`60`、`65` 等数值当成 **百分比** 传入。

例如：

```tsx
<Panel defaultSize={35} minSize={28} maxSize={45}>
```

在当前依赖中的实际语义是默认宽度 35px、最小宽度 28px、最大宽度 45px，而不是 35%、28%、45%。这与截图中列表栏约 35–40px 的实际表现精确吻合。

该问题具有以下特征：

1. **P0 级直接受影响页面**：My Resumes、Jobs、Applications。
2. **与语言无关但会被多语言放大**：英文已经不可读，中文会更容易出现单字纵向排列；繁体中文和较长文案风险更高。
3. **AI Chat 不使用 `react-resizable-panels`，因此不存在同一个根因**；其全局 Drawer 使用 `90vw` 和 `max-width`，源码层面未发现与截图相同的 35–40px 压缩问题，但仍缺少真实浏览器、多视口、多语言布局回归测试，需要在修复阶段同步验证。
4. **现有测试无法发现问题**：当前 MainLayout 测试把页面和 Drawer mock 掉，i18n parity 测试只检查语言键集合及插值变量一致，不检查硬编码英文或实际布局；三个 Master–Detail 容器也没有对应的布局契约测试。
5. **不建议通过回退依赖解决**：Master–Detail 功能在提交 `62e2452d` 引入时，锁文件已经是 `react-resizable-panels@4.11.2`，同属 v4；错误来自调用方沿用了不匹配的尺寸语义，而不是仅由后续 `4.12.1` 升级造成。

## 2. 根因证据链

### 2.1 依赖的实际尺寸语义

当前锁定版本：

- `frontend/package.json`：`react-resizable-panels: ^4.12.1`
- `frontend/package-lock.json`：实际安装 `4.12.1`

安装包 `frontend/node_modules/react-resizable-panels/dist/react-resizable-panels.js` 的尺寸解析逻辑明确显示：

- `number` → `px`
- 以 `%` 结尾的字符串 → 百分比
- 未显式写单位的字符串才回退为百分比

因此，如果期望百分比，应传入 `"35%"`、`"65%"`，而不是 `{35}`、`{65}`。

### 2.2 三个页面使用了错误的数值型尺寸

| 页面 | 文件 | 当前列表栏参数 | 当前详情栏参数 | 实际结果 |
|---|---|---|---|---|
| My Resumes | `frontend/src/pages/resumes/ResumesPage.tsx` | `35 / 28 / 45` | `65 / 40` | 列表栏默认约 35px，最大约 45px |
| Jobs | `frontend/src/pages/jobs/JobsPage.tsx` | `40 / 30 / 50` | `60 / 35` | 列表栏默认约 40px，最大约 50px |
| Applications | `frontend/src/pages/tracking/TrackingPage.tsx` | `40 / 30 / 50` | `60 / 35` | 与 Jobs 相同，尚未在用户截图中展示但必然受同一问题影响 |

三个页面的 Panel wrapper 又使用 `overflow-auto`，所以列表内容在极窄容器中产生：

- 标题和正文逐字/逐词换行；
- 内层横向滚动条；
- 内层纵向滚动条；
- 分隔条紧贴主侧栏，看起来像页面层叠或整体错位。

这些是错误宽度的后果，不是四个独立 CSS bug。

### 2.3 次要响应式风险：断点依据错误

`frontend/src/hooks/useMediaQuery.ts` 当前用浏览器视口 `(min-width: 1024px)` 决定是否进入 Master–Detail 模式，但实际内容宽度还要扣除：

- 展开侧栏：240px；
- 页面容器 padding；
- 分隔条；
- 可能出现的浏览器滚动条。

因此视口刚达到 1024px 时，主内容区只有约 750–780px，却会被强制拆成双栏。即使把百分比语义修正，仍可能出现两个面板都过窄的问题。该问题不是本次截图的直接根因，但应在同一修复中处理，否则会留下 1024–1279px 区间的响应式缺陷。

### 2.4 MainLayout 容器不是本次直接根因

`MainLayout.tsx` 已对右侧 flex 内容使用 `flex-1 min-w-0`，主侧栏也使用固定宽度和 `shrink-0`。截图中主侧栏宽度、详情区占位和空状态居中均基本正常，说明 AppShell 主结构仍在工作。

页面外层的 `container mx-auto px-4 py-6` 与子页面 `-mx-4 -my-6` 组合较脆弱，应在重构时简化，但它不能解释固定的 35–40px 列表栏；Panel 数值像素语义才是决定性根因。

## 3. AI Chat 同步检查结论

### 3.1 与主 bug 的关系

AI Chat 的当前实现路径是：

```text
MainLayout
  -> GlobalCopilotDrawer
     -> SheetContent
        -> CopilotChatArea
```

`GlobalCopilotDrawer.tsx` 使用：

```tsx
className="w-[90vw] max-w-[480px] sm:max-w-[540px] p-0 flex flex-col"
```

它没有使用 `Panel` 或数值型 resizable size，因此不会触发此次 35–40px 的同类错误。`CopilotChatArea` 的主容器使用 `flex flex-col h-full`，顶部选择器已有 `flex-1 min-w-0`，消息区使用独立的纵向滚动；源码层面没有发现与截图相同的结构性压缩。

### 3.2 仍需纳入修复轮验证的风险

AI Chat 尚不能标记为“已通过”，原因如下：

1. 当前 Docker 服务未运行，`http://localhost:80` 返回连接拒绝，无法复用用户的登录态进行真实页面点击验证。
2. `GlobalCopilotDrawer` / `CopilotChatArea` 没有专门的组件布局测试。
3. Drawer 在移动端使用 90vw，而内部新建会话 Dialog、Select 下拉层、上下文标签、消息长文本和输入区需要分别验证是否溢出。
4. 消息正文使用 `whitespace-pre-wrap`，还应增加 `break-words` 或等效策略验证超长 URL、无空格文本和中日韩文本不会制造横向滚动。
5. 新会话简历选项直接显示后端 `version.status` 英文枚举；若产品要求完整本地化，应复用已有状态翻译键，而不是向中文用户暴露原始英文状态。

## 4. 多语言问题

### 4.1 已确认的硬编码英文

以下宽屏空状态没有经过 i18n：

- Resumes：`Select a resume` / `Choose a resume from the list to view details`
- Jobs：`Select a job` / `Choose a job from the list to view details`
- Applications：`Select an application` / `Choose an application from the list to view details`

这意味着当前切换到简体中文或繁体中文后，宽屏详情空状态仍显示英文。现有 `i18nParity.test.ts` 无法发现这种“组件绕过 `t()`”的问题。

### 4.2 修复要求

1. 为三个 Master–Detail 空状态增加语义清晰的翻译键，分别补齐 `en`、`zh-CN`、`zh-TW`。
2. 页面组件统一使用 `useTranslation()`，不保留硬编码用户可见字符串。
3. 保持现有 i18n parity 测试，并新增针对三个页面的语言渲染测试。
4. 用三种语言分别执行桌面、临界宽度和移动端视觉检查；不能只验证 key parity。
5. AI Chat 的状态、占位符、按钮、空状态、错误提示、上下文标签和 Drawer 无障碍标题均纳入同一语言矩阵。

## 5. 详细修复计划

### 阶段 A：最小根因修复（P0）

目标：立即消除 35–40px 面板和页面紊乱。

1. 修改以下文件：
   - `frontend/src/pages/resumes/ResumesPage.tsx`
   - `frontend/src/pages/jobs/JobsPage.tsx`
   - `frontend/src/pages/tracking/TrackingPage.tsx`
2. 将所有代表比例的 Panel 参数改为带 `%` 的字符串，例如：
   - Resumes 默认比例：`defaultSize="35%"` / `defaultSize="65%"`
   - Jobs、Applications 默认比例：`defaultSize="40%"` / `defaultSize="60%"`
3. 不要机械地把所有 min/max 都改成百分比后结束。列表栏应设置可读的像素最小宽度（建议从 320px 开始验证），详情栏也应设置合理的最小宽度；默认值使用百分比，内容安全下限使用显式 `px`，单位必须可读且无歧义。
4. 给 Group、Panel 和关键 panel wrapper 增加稳定的 `data-testid` 或 `data-slot`，供布局测试读取 bounding box；不要依赖 Tailwind class 文本作为测试契约。
5. 保持 Separator 可拖动，并验证拖动后不会越过两侧最小宽度。

### 阶段 B：响应式策略修复（P0/P1）

目标：避免在“视口够宽、实际内容区不够宽”时错误启用双栏。

推荐方案：按 Master–Detail 容器的可用宽度决定模式，而不是仅按 `window.innerWidth`。可使用 `ResizeObserver` 封装一个共享 hook 或共享布局组件。

实施步骤：

1. 计算最低可用宽度：列表最小宽度 + 详情最小宽度 + Separator。
2. 容器低于阈值时沿用现有单页路由模式：列表页显示列表，详情路由显示详情。
3. 容器达到阈值时才挂载 `Group` 双栏。
4. 若短期不引入容器测量，至少把断点提高到能覆盖 240px 展开侧栏后的安全视口，并同时验证侧栏展开/折叠；该方案是过渡方案，不如容器宽度可靠。
5. 把三个页面重复的 Group / Separator / 空状态骨架抽成共享 `MasterDetailLayout`，统一尺寸单位、最小宽度、overflow 和测试标识，防止未来三处再次漂移。

### 阶段 C：滚动和尺寸边界收敛（P1）

目标：每个区域只在预期方向滚动，不出现嵌套横向滚动条。

1. MainLayout 的页面内容区保留 `min-w-0`，并评估为 Master–Detail 路由提供无 `container max-width` 的 full-bleed content slot，替代当前 `container` + 负 margin 抵消方式。
2. Group 和 Panel wrapper 明确使用 `min-w-0 h-full overflow-hidden`。
3. 列表与详情内部只允许内容区 `overflow-y-auto`；除确有横向数据表需求外，禁止顶层 `overflow-auto`。
4. 标题、按钮组、筛选栏、卡片元数据分别定义 `truncate`、`break-words`、换行或收缩策略。
5. 验证浏览器根节点和主内容区满足 `scrollWidth <= clientWidth`。

### 阶段 D：国际化补齐（P1）

目标：修复后在英文、简体中文、繁体中文下都保持完整且可读。

1. 在三个 locale JSON 中增加三个页面的 Master–Detail 空状态键。
2. 三个页面引入 `useTranslation()` 并替换硬编码英文。
3. 检查 AI Chat 简历版本状态是否已有可复用翻译键；如没有，建立统一的 resume status/parse status 映射。
4. 扩展测试：除 key parity 外，分别用 `en`、`zh-CN`、`zh-TW` 渲染三个空状态与 AI Chat 核心 UI。
5. 使用真实语言文本测试，不使用长度过短的占位 stub；繁体中文和英文长文案必须覆盖。

### 阶段 E：AI Chat 布局加固（P1）

目标：确认 AI Chat 没有同类或独立的窄屏布局问题。

1. 为 `GlobalCopilotDrawer` 添加测试，验证手机为约 90vw、桌面不超过 540px，且关闭按钮不覆盖标题或顶部选择器。
2. 为 `CopilotChatArea` 的顶部会话选择、新建按钮、上下文标签、消息区、输入区增加 `min-w-0` / `break-words` 等必要约束。
3. 分别验证：无会话、有会话、超长会话标题、超长简历名/公司名、超长无空格消息、等待响应、compact 提示、新建会话 Dialog 和 Select 展开状态。
4. 从侧栏 AI Chat 按钮和 `/chat` 路由两种入口验证 Drawer；从 Job Detail 的 Ask Copilot 入口验证携带 Job context 时布局不变。
5. 手机端同时验证 Drawer、Dialog 和软键盘高度变化下输入区仍可见。

## 6. 测试与验证计划

### 6.1 自动化测试

1. **组件契约测试**
   - mock `react-resizable-panels`，断言传入尺寸带明确单位；
   - 防止重新出现数值型 `{35}` / `{40}`；
   - 三个页面均覆盖，不只测 Resumes。
2. **i18n 测试**
   - 保留 locale key parity；
   - 新增三个页面空状态的三语言渲染断言；
   - 增加用户可见硬编码英文的 lint/静态检查策略或针对性测试。
3. **浏览器布局测试（Playwright）**
   - 读取 master panel、detail panel、separator、main content 的 bounding box；
   - 断言列表栏不小于设计下限；
   - 断言两个 Panel 不重叠，Separator 位于二者之间；
   - 断言页面没有非预期横向溢出；
   - 拖动 Separator 后再次断言 min/max 边界。
4. **AI Chat 测试**
   - Drawer 宽度边界；
   - 内部控件不越界；
   - 长文本不制造横向滚动；
   - Dialog/Select portal 在 Drawer 上方正确显示。

### 6.2 视口矩阵

| 类型 | 建议视口 | 侧栏状态 | 预期 |
|---|---:|---|---|
| 大桌面 | 1920×1080 | 展开/折叠 | 双栏，比例正确，无横向滚动 |
| 常规桌面 | 1440×900 | 展开/折叠 | 双栏，列表可读 |
| 小型桌面 | 1280×800 | 展开/折叠 | 根据实际可用宽度决定双栏或单栏 |
| 临界宽度 | 1024×768 | 展开/折叠 | 不允许在不足空间中强制双栏 |
| 平板 | 768×1024 | 移动侧栏 | 单栏路由模式 |
| 手机 | 390×844、375×667 | Sheet 侧栏 | 单栏；AI Drawer、Dialog、输入区可用 |

每个视口至少覆盖：`en`、`zh-CN`、`zh-TW`，以及 Resumes、Jobs、Applications、AI Chat 四个功能区。

### 6.3 数据状态矩阵

1. 空列表 / 有多条数据；
2. 未选择详情 / 已选择详情；
3. 短标题 / 超长标题；
4. 正常英文单词 / 中文 / 繁体中文 / 无空格长字符串；
5. 加载中 / 错误 / 成功；
6. AI Chat 无会话 / 有消息 / 长消息 / 新建会话弹窗。

## 7. 验收标准

修复只有同时满足以下条件才能关闭：

1. My Resumes、Jobs、Applications 在桌面宽屏下的列表栏不再是 35–40px，初始比例符合设计值。
2. 三个页面在 Separator 拖动前后均不发生 Panel 重叠或内容穿透。
3. 1024px 附近不会因为主侧栏占宽而强制进入不可用的双栏模式。
4. 页面顶层无非预期横向滚动；列表和详情只在各自内容区纵向滚动。
5. 英文、简体中文、繁体中文下，空状态、标题、按钮、筛选项均完整显示且不出现逐字纵排。
6. AI Chat 从侧栏、`/chat`、Job Detail 三个入口打开均布局正常；手机和桌面宽度均无横向溢出。
7. Panel 尺寸单位契约、三语言文案和关键布局均有自动化回归测试。
8. `npm run type-check`、前端单元测试和 Playwright 布局回归全部通过。

## 8. 本轮已执行验证与限制

已执行：

- 检查当前分支及工作区状态：`post_review_july`，定位开始时工作区无未提交改动。
- 复核用户截图并将可见宽度与 Panel 配置对照。
- 检查当前安装包的尺寸解析源码，确认数字按 px 处理。
- 检查 Resumes、Jobs、Applications、MainLayout、AI Chat Drawer、CopilotChatArea 和三套 locale。
- 运行 `npm.cmd run type-check`：通过。
- 运行 MainLayout 与 i18n parity 定向测试：2 个测试文件、7 个测试全部通过；该结果同时证明现有测试没有覆盖真实 Panel 布局。

限制：

- 定位时 Docker Compose 没有正在运行的容器，`localhost:80` 不可访问，因此没有把 AI Chat 标记为运行态已通过。
- 本轮按要求只生成定位与修复计划，没有修改前端实现，也没有伪称修复完成。

## 9. 建议实施顺序

```text
显式尺寸单位修复
  -> 三页面布局契约测试
  -> 实际内容宽度断点修复
  -> overflow/full-bleed 布局收敛
  -> 三语言空状态补齐
  -> AI Chat 多视口与长文本加固
  -> Docker 全链路人工回归 + Playwright 回归
```

优先先落地 P0 的尺寸语义修复并马上在 Docker 中验证截图场景；随后在同一修复轮处理 Applications、临界宽度和 i18n，避免只修用户已经点击过的两个入口而留下同源缺陷。

---

## 10. 修复执行报告（2026-07-12）

### 10.1 根因二次确认

在动手前先核对了安装包的类型声明 `frontend/node_modules/react-resizable-panels/dist/react-resizable-panels.d.ts`，`Panel` 的尺寸语义被明确写为：

- 数值型 → 像素（px）
- 无单位字符串 → 百分比
- `"33%"` 这类字符串 → 百分比

这与定位阶段结论完全一致：历史代码传 `defaultSize={35}` 被库当作 35px，正是列表栏被压成约 35–40px 的直接原因。安装版本确认为 `react-resizable-panels@4.12.1`（bvaughn 官方包，v4 新 API：`Group` / `Panel` / `Separator`）。

### 10.2 各阶段实际改动

阶段 A（P0 尺寸单位修复）与阶段 B（响应式策略）合并落地为一个共享布局组件，避免三处再次漂移：

- 新增 `frontend/src/hooks/useContainerWidth.ts`
  - 基于 `ResizeObserver` 测量容器真实宽度，`useLayoutEffect` 在绘制前同步纠正，规避布局闪烁；对不支持 `ResizeObserver` 的环境做了降级保护。
- 新增 `frontend/src/components/layout/MasterDetailLayout.tsx`
  - 默认宽度用百分比字符串（Resumes `35%` / `65%`，Jobs、Applications `40%` / `60%`）。
  - 内容安全下限用显式像素字符串（列表默认 `340px`，详情默认 `460px`），列表最大宽度用 `55%`，单位可读无歧义。
  - 是否分栏改为按“容器实际可用宽度”判断：`listMinPx + detailMinPx + Separator`（默认阈值约 806px）；不足时回退到既有单栏路由模式。这样在视口 1024px、展开 240px 侧栏导致内容区不足时不会强制双栏。
  - 为 `Group` / `Panel` / `Separator` 提供由 `id` 派生的稳定 `data-testid`，容器加 `data-slot`，供布局回归读取 bounding box，不再以 Tailwind class 作为测试契约。
  - Group / Panel 统一 `min-w-0 h-full overflow-hidden`。
- 新增 `frontend/src/components/layout/MasterDetailEmpty.tsx`
  - 详情栏空状态统一走 i18n，并使用 `break-words` 防止长文案与中日韩逐字纵排。
- 重写 `ResumesPage.tsx`、`JobsPage.tsx`、`TrackingPage.tsx`：三页统一改用 `MasterDetailLayout` + `MasterDetailEmpty`，删除各自内联的 `Group`/`Panel` 与硬编码英文空状态。窄屏回退行为保持不变（列表/详情按选中态切换）。

阶段 C（滚动与尺寸边界收敛）：

- 六个 Panel wrapper（`ResumeListPanel` / `ResumeDetailPanel` / `JobListPanel` / `JobDetailPanel` / `TrackingListPanel` / `TrackingDetailPanel`）由 `overflow-auto` 收敛为 `min-w-0 overflow-y-auto`，只允许纵向滚动，消除嵌套横向滚动条。

阶段 D（国际化补齐）：

- 三套 locale 新增 `masterDetail.resume` / `masterDetail.job` / `masterDetail.application` 的 `emptyTitle` / `emptyDesc`（`en`、`zh-CN`、`zh-TW`），三页空状态不再出现硬编码英文。
- AI Chat 新会话简历选项的 `version.status`（`ACTIVE` / `ARCHIVED`）改为复用已有 `resume.timeline.active` / `resume.timeline.archived` 翻译键，不再向中文用户暴露原始英文枚举。

阶段 E（AI Chat 布局加固）：

- `CopilotChatArea` 根容器与消息气泡补 `min-w-0`，消息正文 `whitespace-pre-wrap` 追加 `break-words`，防止超长 URL / 无空格文本 / 中日韩长文本制造横向滚动。
- `GlobalCopilotDrawer` 的 `SheetContent` 增加 `min-w-0` 与 `data-testid`，保留 `90vw` + `max-width` 宽度约束。

### 10.3 自动化回归

新增测试：

- `frontend/src/components/layout/MasterDetailLayout.test.tsx`（7 项）
  - mock `react-resizable-panels`，断言默认宽度为 `%`、内容下限为 `px`；
  - 显式回归守卫：禁止再次出现纯数值型尺寸（会被库当 px）；
  - 覆盖宽屏双栏、空状态/详情切换、窄屏单栏列表、窄屏单栏详情。
- `frontend/src/components/layout/MasterDetailEmpty.test.tsx`（5 项）
  - 用真实 i18n 实例分别以 `en` / `zh-CN` / `zh-TW` 渲染三页空状态，断言译文正确。

保留并通过既有 `i18nParity.test.ts`（key 与插值变量一致性）与 `MainLayout.test.tsx`。

### 10.4 本轮验证结果

- `npm run type-check`：通过（exit 0）。
- 前端单元测试全量 `vitest run`：**54 个测试文件、480 个测试全部通过**（exit 0）。
- 新增布局与 i18n 定向测试：12 项全部通过。
- `eslint` 对全部改动文件：无告警、无错误（exit 0）。

### 10.5 验收标准对照

| # | 验收标准 | 状态 | 说明 |
|---|---|---|---|
| 1 | 三页列表栏不再是 35–40px，初始比例符合设计 | 已修复（源码/单测层面） | 默认 `35%`/`40%` 百分比 + `340px` 下限；单测断言尺寸单位契约 |
| 2 | 拖动前后不发生 Panel 重叠/穿透 | 已加固 | min/max 约束 + `min-w-0 overflow-hidden`；未做真实拖动回归 |
| 3 | 1024px 附近不强制不可用双栏 | 已修复 | 改为按容器可用宽度判断，阈值约 806px |
| 4 | 顶层无非预期横向滚动，仅内容区纵向滚动 | 已加固 | wrapper 收敛为 `overflow-y-auto`；未做真实浏览器溢出测量 |
| 5 | 三语言空状态/文案完整可读 | 已修复 | 新增 `masterDetail.*` 三语言键并接入，单测覆盖 |
| 6 | AI Chat 三入口布局正常、无横向溢出 | 部分（源码加固） | 已补 `min-w-0`/`break-words`；未做真实浏览器/多入口回归 |
| 7 | 尺寸单位、三语言、关键布局有自动化回归 | 已达成（单测层面） | 新增尺寸契约与 i18n 渲染测试；Playwright 未执行 |
| 8 | type-check + 单测 + Playwright 全部通过 | 部分 | type-check、单测通过；Playwright 未执行 |

### 10.6 限制与后续建议

- 本环境 Docker Compose 未运行，`localhost:80` 不可访问，且无法启动长时开发服务，因此**未执行真实浏览器 Playwright 布局回归与多视口/多语言人工回归**。第 6 节的视口矩阵、拖动 Separator 边界、页面顶层 `scrollWidth <= clientWidth` 等断言仍待在可运行环境中补齐。
- 建议后续在能运行浏览器时补充：
  1. Playwright 读取 `*-container` / `*-list` / `*-detail` / `*-separator` bounding box 的布局断言；
  2. 拖动 Separator 后的 min/max 边界回归；
  3. AI Chat 从侧栏、`/chat`、Job Detail 三入口的桌面/手机布局与长文本溢出回归。
- `frontend/src/hooks/useMediaQuery.ts` 中的 `useMasterDetail` 现已无调用方（分栏判断改为容器宽度）。本轮保留未删除以降低风险，可在后续清理中移除。

---

## 11. Codex 复审与剩余问题修复报告（2026-07-12）

### 11.1 复审发现

首次修复已经正确解决 Panel 数值被解释为 px 的直接根因，但代码审查发现两个仍可能影响真实界面的残余问题：

1. `MasterDetailLayout` 已按容器宽度决定单双栏，但被嵌入的 Resume、Jobs、Applications 列表仍使用 `sm` / `md` / `lg` 浏览器视口断点。桌面浏览器中的列表 Panel 即使只有 340–500px，也可能启用三列简历卡片、横向 Jobs 筛选栏或四列 Applications 统计卡。
2. 六个 Panel wrapper 只设置了 `overflow-y-auto`。CSS 中仅设置 Y 轴为 `auto` 并不能保证 X 轴被禁止，因此“已经消除横向滚动”的表述过强。
3. 首次新增测试 mock 了宽度 hook 和 resizable panel 库，能够验证尺寸单位契约，但没有执行真实的 `ResizeObserver` 生命周期，也没有浏览器 bounding box / overflow 断言。

### 11.2 剩余代码修复

#### 面板内部容器响应式

- 三个列表 Panel wrapper 增加 `master-detail-list-container`，在 `frontend/src/index.css` 中使用浏览器原生 `container-type: inline-size`。
- 使用原生 `@container` 规则覆盖 Panel 内部的 viewport 样式；不依赖当前 Tailwind 配置未启用的 container-query 插件。
- Resume：
  - 窄 Panel 中标题区回到纵向排列、上传按钮占满可用宽度；
  - 简历卡片网格按 Panel 宽度在 1 / 2 / 3 列之间切换；
  - ResumeCard 的标题容器增加 `min-w-0`，允许长标题正确截断。
- Jobs：
  - 标题区、筛选栏、筛选控件和 JobCard 操作区按 Panel 宽度折行；
  - 420px 以下两个固定宽度 Select 改为纵向、100% 宽度；
  - Job 标题增加 `min-w-0` / `break-words`，避免长职位名撑开卡片。
- Applications：
  - 统计卡按 Panel 宽度切换 1 / 2 / 4 列；
  - 记录行、状态和操作区在窄 Panel 中改为纵向/换行布局；
  - 职位标题和内容容器增加 `min-w-0` / `break-words`。

#### 横向溢出封口

- 六个列表/详情 Panel wrapper 统一使用 `overflow-x-hidden overflow-y-auto`。
- 内部固定宽度控件先通过 container query 折行或改为全宽，再由 wrapper 阻止非预期的页面横向滚动，避免简单隐藏导致操作控件被裁切。

### 11.3 新增和完善的测试

- 新增 `frontend/src/hooks/useContainerWidth.test.tsx`（4 项）：
  - 首次 layout effect 测量；
  - ResizeObserver 尺寸变化更新；
  - unmount 时 observer 清理；
  - 不支持 ResizeObserver 时仍执行首次测量。
- 新增 `frontend/src/components/layout/MasterDetailPanels.test.tsx`（3 项）：
  - Resumes、Jobs、Applications 三个列表 Panel 均建立 container；
  - 明确断言 `overflow-x-hidden` 和 `overflow-y-auto`。
- 新增 `frontend/src/components/layout/masterDetailResponsiveCss.test.ts`（1 项）：
  - 保护原生 container query 入口及 Resume / Jobs / Applications 的关键响应式 selector，避免再次出现“class 存在但构建不生成 CSS”的假修复。
- 扩展 `frontend/acceptance/full-stack.acceptance.spec.ts`：
  - 1440×900 下读取三个页面的 list / separator / detail bounding box，检查最小宽度、排列顺序和页面横向溢出；
  - 1024×768 下断言三个页面回退为单栏，不挂载 Group；
  - 390×844 下检查 AI Drawer 宽度和内部横向溢出。

### 11.4 最终验证结果

- 前端 Vitest 全量回归：**57 个测试文件、488 个测试全部通过**。
- 新增的容器测量、Panel wrapper 和 CSS 契约测试：8 项全部通过。
- `npm.cmd run type-check`：通过。
- `npm.cmd run lint`：通过，无 ESLint 错误。
- `npm.cmd run build`：通过；生产 CSS 从首次修复后的 87.44kB 增长到 88.93kB，且哈希变化，确认原生 container query 规则已进入构建产物。
- `npx.cmd playwright test -c playwright.acceptance.config.ts --list`：通过，新增 acceptance 断言可被 Playwright 正常发现和解析。
- Docker Compose 当前无运行容器，因此依赖完整后端和认证数据的 acceptance 用例本轮未实际执行；不可将其记录为“Playwright 已通过”。

### 11.5 最终验收状态

| 项目 | 状态 |
|---|---|
| Panel px/% 直接根因 | 已修复 |
| 三页面共享布局和容器宽度断点 | 已修复 |
| Panel 内部按实际面板宽度响应 | 已修复（源码、CSS 契约和构建产物已验证） |
| 非预期横向滚动封口 | 已修复（源码和单测已验证） |
| 英文、简体中文、繁体中文空状态 | 已修复并有渲染测试 |
| AI Chat 长文本和 Drawer 宽度加固 | 已修复；真实多入口浏览器回归仍待运行环境验证 |
| Vitest / type-check / lint / build | 已通过 |
| 全栈 Playwright | 用例已完善且可发现；因 Docker 未运行，未执行 |
