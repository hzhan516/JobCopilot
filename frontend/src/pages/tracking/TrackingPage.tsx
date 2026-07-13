import { useParams } from 'react-router-dom';
import Tracking from '@/pages/tracking/Tracking';
import TrackingListPanel from '@/pages/tracking/components/TrackingListPanel';
import TrackingDetailPanel from '@/pages/tracking/components/TrackingDetailPanel';
import MasterDetailLayout from '@/components/layout/MasterDetailLayout';
import MasterDetailEmpty from '@/components/layout/MasterDetailEmpty';
import { ClipboardList } from 'lucide-react';

/**
 * Tracking Master-Detail 页面容器。
 * 宽屏分栏：左侧 Tracking 列表 + 右侧详情
 * 窄屏回退：/applications 显示列表，/applications/:trackingId 显示详情
 *
 * 是否分栏由 MasterDetailLayout 依据实际可用内容宽度决定（而非视口宽度）。
 */
export default function TrackingPage() {
  const { trackingId } = useParams<{ trackingId: string }>();

  return (
    <MasterDetailLayout
      groupId="tracking-master-detail"
      defaultListPercent={40}
      hasSelection={!!trackingId}
      list={<TrackingListPanel />}
      detail={<TrackingDetailPanel />}
      emptyDetail={
        <MasterDetailEmpty
          icon={ClipboardList}
          titleKey="masterDetail.application.emptyTitle"
          descKey="masterDetail.application.emptyDesc"
        />
      }
      narrowList={<Tracking />}
      narrowDetail={<Tracking selectedTrackingId={trackingId} />}
    />
  );
}
