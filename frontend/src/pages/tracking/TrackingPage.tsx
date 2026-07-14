import { useParams } from 'react-router-dom';
import TrackingListPanel from '@/pages/tracking/components/TrackingListPanel';
import TrackingDetailPanel from '@/pages/tracking/components/TrackingDetailPanel';

/**
 * 中央工作区使用路由切换列表/详情，不再在 5fr 内容栏内二次横向切分。
 */
export default function TrackingPage() {
  const { trackingId } = useParams<{ trackingId: string }>();

  return (
    <div data-testid="tracking-route-layout" className="h-full min-w-0">
      {trackingId ? <TrackingDetailPanel /> : <TrackingListPanel />}
    </div>
  );
}
