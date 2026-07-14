import ResumeList from '@/pages/resumes/ResumeList';

/**
 * Master-Detail 左侧面板 — 直接复用现有 ResumeList。
 */
export default function ResumeListPanel() {
  return (
    <div className="master-detail-list-container h-full min-w-0 overflow-x-hidden overflow-y-auto">
      <ResumeList />
    </div>
  );
}
