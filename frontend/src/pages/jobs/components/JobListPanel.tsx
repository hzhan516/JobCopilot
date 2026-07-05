import JobList from '@/pages/jobs/JobList';

/**
 * Master-Detail 左侧面板 — 直接复用现有 JobList。
 * 在宽屏分栏模式下作为列表面板呈现。
 */
export default function JobListPanel() {
  return (
    <div className="h-full overflow-auto">
      <JobList />
    </div>
  );
}
