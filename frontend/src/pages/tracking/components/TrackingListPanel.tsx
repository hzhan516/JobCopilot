import Tracking from '@/pages/tracking/Tracking';

/**
 * Master-Detail 左侧面板 — 直接复用现有 Tracking（列表模式）。
 */
export default function TrackingListPanel() {
  return (
    <div className="h-full overflow-auto">
      <Tracking />
    </div>
  );
}
