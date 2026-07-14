import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import MasterDetailLayout from './MasterDetailLayout';

// 可控的容器宽度（px）。null 表示尚未测量。
const widthRef = vi.hoisted(() => ({ value: 2000 as number | null }));

vi.mock('@/hooks/useContainerWidth', () => ({
  useContainerWidth: () => [{ current: null }, widthRef.value],
}));

// 用轻量 mock 捕获传给 Panel 的尺寸单位，验证尺寸契约（不再出现数值型 px 语义）。
vi.mock('react-resizable-panels', () => ({
  Group: ({ children, id }: any) => <div data-testid={id}>{children}</div>,
  Panel: ({ children, id, defaultSize, minSize, maxSize }: any) => (
    <div
      data-testid={id}
      data-default-size={String(defaultSize)}
      data-min-size={String(minSize)}
      data-max-size={String(maxSize)}
    >
      {children}
    </div>
  ),
  Separator: ({ id }: any) => <div data-testid={id} />,
}));

function renderLayout(props?: Partial<React.ComponentProps<typeof MasterDetailLayout>>) {
  return render(
    <MasterDetailLayout
      groupId="jobs-master-detail"
      defaultListPercent={40}
      hasSelection={false}
      list={<div data-testid="list-content">list</div>}
      detail={<div data-testid="detail-content">detail</div>}
      emptyDetail={<div data-testid="empty-content">empty</div>}
      narrowList={<div data-testid="narrow-list">narrow-list</div>}
      narrowDetail={<div data-testid="narrow-detail">narrow-detail</div>}
      {...props}
    />
  );
}

describe('MasterDetailLayout', () => {
  beforeEach(() => {
    widthRef.value = 2000;
  });

  it('renders a two-panel Group when the container is wide enough', () => {
    renderLayout();
    expect(screen.getByTestId('jobs-master-detail')).toBeInTheDocument();
    expect(screen.getByTestId('jobs-master-detail-list')).toBeInTheDocument();
    expect(screen.getByTestId('jobs-master-detail-detail')).toBeInTheDocument();
    expect(screen.getByTestId('jobs-master-detail-separator')).toBeInTheDocument();
  });

  it('passes percentage-based default sizes and pixel-based min sizes (unit contract)', () => {
    renderLayout({ defaultListPercent: 40, listMinPx: 340, detailMinPx: 460, maxListPercent: 55 });

    const listPanel = screen.getByTestId('jobs-master-detail-list');
    const detailPanel = screen.getByTestId('jobs-master-detail-detail');

    // 默认宽度必须是百分比字符串
    expect(listPanel.getAttribute('data-default-size')).toBe('40%');
    expect(detailPanel.getAttribute('data-default-size')).toBe('60%');

    // 内容下限必须是显式像素字符串
    expect(listPanel.getAttribute('data-min-size')).toBe('340px');
    expect(detailPanel.getAttribute('data-min-size')).toBe('460px');

    // 列表最大宽度使用百分比
    expect(listPanel.getAttribute('data-max-size')).toBe('55%');
  });

  it('never passes a bare numeric size to Panel (regression guard for px/% confusion)', () => {
    renderLayout();
    for (const testId of ['jobs-master-detail-list', 'jobs-master-detail-detail']) {
      const panel = screen.getByTestId(testId);
      for (const attr of ['data-default-size', 'data-min-size']) {
        const value = panel.getAttribute(attr) ?? '';
        // 必须带单位；纯数字（会被库当作 px）视为回归
        expect(value).toMatch(/(%|px)$/);
        expect(value).not.toMatch(/^\d+$/);
      }
    }
  });

  it('shows the empty detail when nothing is selected in wide mode', () => {
    renderLayout({ hasSelection: false });
    expect(screen.getByTestId('empty-content')).toBeInTheDocument();
    expect(screen.queryByTestId('detail-content')).not.toBeInTheDocument();
  });

  it('shows the detail content when something is selected in wide mode', () => {
    renderLayout({ hasSelection: true });
    expect(screen.getByTestId('detail-content')).toBeInTheDocument();
    expect(screen.queryByTestId('empty-content')).not.toBeInTheDocument();
  });

  it('falls back to the single-pane list when the container is too narrow', () => {
    widthRef.value = 500; // < 340 + 460 + separator
    renderLayout({ hasSelection: false });
    expect(screen.getByTestId('narrow-list')).toBeInTheDocument();
    expect(screen.queryByTestId('jobs-master-detail')).not.toBeInTheDocument();
  });

  it('falls back to the single-pane detail when narrow and an item is selected', () => {
    widthRef.value = 500;
    renderLayout({ hasSelection: true });
    expect(screen.getByTestId('narrow-detail')).toBeInTheDocument();
    expect(screen.queryByTestId('jobs-master-detail')).not.toBeInTheDocument();
  });
});
