import { useParams } from 'react-router-dom';
import { useMasterDetail } from '@/hooks/useMediaQuery';
import Tracking from '@/pages/tracking/Tracking';
import TrackingListPanel from '@/pages/tracking/components/TrackingListPanel';
import TrackingDetailPanel from '@/pages/tracking/components/TrackingDetailPanel';
import { Panel, Group, Separator } from 'react-resizable-panels';
import { ClipboardList } from 'lucide-react';

/**
 * Tracking Master-Detail 页面容器。
 * ≥1024px 分栏：左侧 Tracking 列表 + 右侧详情
 * <1024px  回退路由跳转：/applications 显示列表，/applications/:trackingId 显示详情
 */
export default function TrackingPage() {
  const isWide = useMasterDetail();
  const { trackingId } = useParams<{ trackingId: string }>();

  // 窄屏模式：零修改复用现有全屏组件
  if (!isWide) {
    if (trackingId) return <Tracking selectedTrackingId={trackingId} />;
    return <Tracking />;
  }

  // 宽屏 Master-Detail 模式
  return (
    <div className="-mx-4 -my-6 h-[calc(100vh-3.5rem)]">
      <Group orientation="horizontal" id="tracking-master-detail">
        {/* 左侧列表 */}
        <Panel defaultSize={40} minSize={30} maxSize={50}>
          <TrackingListPanel />
        </Panel>

        <Separator className="w-1.5 bg-border hover:bg-blue-400 transition-colors active:bg-blue-500" />

        {/* 右侧详情 */}
        <Panel defaultSize={60} minSize={35}>
          {trackingId ? (
            <TrackingDetailPanel />
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-muted-foreground bg-muted/20">
              <ClipboardList className="w-16 h-16 mb-4 opacity-30" />
              <p className="text-lg font-medium">Select an application</p>
              <p className="text-sm mt-1">Choose an application from the list to view details</p>
            </div>
          )}
        </Panel>
      </Group>
    </div>
  );
}
