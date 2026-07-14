import { useParams } from 'react-router-dom';
import JobListPanel from '@/pages/jobs/components/JobListPanel';
import JobDetailPanel from '@/pages/jobs/components/JobDetailPanel';

/**
 * 中央工作区使用路由切换列表/详情，不再在 5fr 内容栏内二次横向切分。
 */
export default function JobsPage() {
  const { jobId } = useParams<{ jobId: string }>();

  return (
    <div data-testid="jobs-route-layout" className="h-full min-w-0">
      {jobId ? <JobDetailPanel /> : <JobListPanel />}
    </div>
  );
}
