import { useParams } from 'react-router-dom';
import Tracking from '@/pages/tracking/Tracking';

/**
 * Master-Detail 右侧面板 — 渲染 Tracking 详情。
 * 通过 URL param 传递选中项。
 */
export default function TrackingDetailPanel() {
  const { trackingId } = useParams<{ trackingId: string }>();

  return (
    <div className="h-full overflow-auto">
      <Tracking selectedTrackingId={trackingId} />
    </div>
  );
}
