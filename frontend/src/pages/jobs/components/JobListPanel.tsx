import JobList from '@/pages/jobs/JobList';

/**
 * Master-Detail 左侧面板 — 直接复用现有 JobList。
 * 在宽屏分栏模式下作为列表面板呈现。
 */
export default function JobListPanel() {
  return (
    <div className="master-detail-list-container h-full min-w-0 overflow-x-hidden overflow-y-auto">
      <JobList />
    </div>
  );
}
