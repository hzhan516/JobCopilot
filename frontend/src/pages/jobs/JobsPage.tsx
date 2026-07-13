import { useParams } from 'react-router-dom';
import JobList from '@/pages/jobs/JobList';
import JobDetail from '@/pages/jobs/JobDetail';
import JobListPanel from '@/pages/jobs/components/JobListPanel';
import JobDetailPanel from '@/pages/jobs/components/JobDetailPanel';
import MasterDetailLayout from '@/components/layout/MasterDetailLayout';
import MasterDetailEmpty from '@/components/layout/MasterDetailEmpty';
import { Briefcase } from 'lucide-react';

/**
 * Jobs Master-Detail 页面容器。
 * 宽屏分栏：左侧 JobList + 右侧 JobDetail
 * 窄屏回退：/jobs 显示列表，/jobs/:jobId 显示详情
 *
 * 是否分栏由 MasterDetailLayout 依据实际可用内容宽度决定（而非视口宽度）。
 */
export default function JobsPage() {
  const { jobId } = useParams<{ jobId: string }>();

  return (
    <MasterDetailLayout
      groupId="jobs-master-detail"
      defaultListPercent={40}
      hasSelection={!!jobId}
      list={<JobListPanel />}
      detail={<JobDetailPanel />}
      emptyDetail={
        <MasterDetailEmpty
          icon={Briefcase}
          titleKey="masterDetail.job.emptyTitle"
          descKey="masterDetail.job.emptyDesc"
        />
      }
      narrowList={<JobList />}
      narrowDetail={<JobDetail />}
    />
  );
}
