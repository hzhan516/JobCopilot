import type { ReactNode } from 'react';
import { Panel, Group, Separator } from 'react-resizable-panels';
import { useContainerWidth } from '@/hooks/useContainerWidth';

/** Separator 视觉/命中宽度（px），用于计算双栏所需的最小可用宽度。 */
const SEPARATOR_PX = 6;

interface MasterDetailLayoutProps {
  /** Group 唯一 id，同时用于派生 Panel/Separator 的 data-testid。 */
  groupId: string;
  /** 当前是否已选中某条详情（决定窄屏渲染列表还是详情）。 */
  hasSelection: boolean;
  /** 宽屏左侧列表面板内容。 */
  list: ReactNode;
  /** 宽屏右侧详情面板内容（已选中时）。 */
  detail: ReactNode;
  /** 宽屏右侧未选中时的空状态。 */
  emptyDetail: ReactNode;
  /** 窄屏（单栏）列表视图。 */
  narrowList: ReactNode;
  /** 窄屏（单栏）详情视图。 */
  narrowDetail: ReactNode;
  /** 列表栏默认宽度百分比。 */
  defaultListPercent?: number;
  /** 列表栏最大宽度百分比。 */
  maxListPercent?: number;
  /** 列表栏内容安全下限（px）。 */
  listMinPx?: number;
  /** 详情栏内容安全下限（px）。 */
  detailMinPx?: number;
}

/**
 * 共享的 Master-Detail 分栏容器。
 *
 * 关键修复点：
 * 1. 尺寸单位语义正确 —— 默认宽度用百分比字符串（`"35%"`），内容下限用像素字符串（`"340px"`）。
 *    react-resizable-panels v4 将数值型 size 解释为像素，历史代码传 `{35}` 会导致列表栏被压成 ~35px。
 * 2. 按“实际可用内容宽度”而非浏览器视口宽度决定是否分栏，避免在展开侧栏后内容区不足时强制双栏。
 * 3. 统一 min-w-0 / overflow-hidden，收敛嵌套横向滚动。
 * 4. 为 Group / Panel / Separator 提供稳定 data-testid（由 id 派生）与 data-slot，供布局回归测试读取 bounding box。
 */
export default function MasterDetailLayout({
  groupId,
  hasSelection,
  list,
  detail,
  emptyDetail,
  narrowList,
  narrowDetail,
  defaultListPercent = 38,
  maxListPercent = 55,
  listMinPx = 340,
  detailMinPx = 460,
}: MasterDetailLayoutProps) {
  const [ref, width] = useContainerWidth<HTMLDivElement>();

  const threshold = listMinPx + detailMinPx + SEPARATOR_PX;
  // 首次渲染尚未测量到容器宽度时，回退到视口宽度以避免布局闪烁；
  // useLayoutEffect 会在浏览器绘制前用真实容器宽度纠正。
  const availableWidth = width ?? (typeof window !== 'undefined' ? window.innerWidth : 0);
  const isWide = availableWidth >= threshold;

  return (
    <div
      ref={ref}
      data-testid={`${groupId}-container`}
      data-slot="master-detail-container"
      className="-mx-4 -my-6 h-[calc(100vh-3.5rem)] min-w-0"
    >
      {isWide ? (
        <Group orientation="horizontal" id={groupId} className="h-full min-w-0">
          <Panel
            id={`${groupId}-list`}
            defaultSize={`${defaultListPercent}%`}
            minSize={`${listMinPx}px`}
            maxSize={`${maxListPercent}%`}
            className="min-w-0 h-full overflow-hidden"
          >
            {list}
          </Panel>

          <Separator
            id={`${groupId}-separator`}
            className="w-1.5 bg-border hover:bg-blue-400 transition-colors active:bg-blue-500"
          />

          <Panel
            id={`${groupId}-detail`}
            defaultSize={`${100 - defaultListPercent}%`}
            minSize={`${detailMinPx}px`}
            className="min-w-0 h-full overflow-hidden"
          >
            {hasSelection ? detail : emptyDetail}
          </Panel>
        </Group>
      ) : hasSelection ? (
        narrowDetail
      ) : (
        narrowList
      )}
    </div>
  );
}
