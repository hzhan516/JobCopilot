import { useParams } from 'react-router-dom';
import { useMasterDetail } from '@/hooks/useMediaQuery';
import JobList from '@/pages/jobs/JobList';
import JobDetail from '@/pages/jobs/JobDetail';
import JobListPanel from '@/pages/jobs/components/JobListPanel';
import JobDetailPanel from '@/pages/jobs/components/JobDetailPanel';
import { Panel, Group, Separator } from 'react-resizable-panels';
import { Briefcase } from 'lucide-react';

/**
 * Jobs Master-Detail 页面容器。
 * ≥1024px 分栏：左侧 JobList + 右侧 JobDetail
 * <1024px  回退路由跳转：/jobs 显示列表，/jobs/:jobId 显示详情
 */
export default function JobsPage() {
  const isWide = useMasterDetail();
  const { jobId } = useParams<{ jobId: string }>();

  // 窄屏模式：零修改复用现有全屏组件
  if (!isWide) {
    if (jobId) return <JobDetail />;
    return <JobList />;
  }

  // 宽屏 Master-Detail 模式
  return (
    <div className="-mx-4 -my-6 h-[calc(100vh-3.5rem)]">
      <Group orientation="horizontal" id="jobs-master-detail">
        {/* 左侧列表 */}
        <Panel defaultSize={40} minSize={30} maxSize={50}>
          <JobListPanel />
        </Panel>

        <Separator className="w-1.5 bg-border hover:bg-blue-400 transition-colors active:bg-blue-500" />

        {/* 右侧详情 */}
        <Panel defaultSize={60} minSize={35}>
          {jobId ? (
            <JobDetailPanel />
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-muted-foreground bg-muted/20">
              <Briefcase className="w-16 h-16 mb-4 opacity-30" />
              <p className="text-lg font-medium">Select a job</p>
              <p className="text-sm mt-1">Choose a job from the list to view details</p>
            </div>
          )}
        </Panel>
      </Group>
    </div>
  );
}
