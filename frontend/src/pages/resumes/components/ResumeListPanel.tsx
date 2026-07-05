import ResumeList from '@/pages/resumes/ResumeList';

/**
 * Master-Detail 左侧面板 — 直接复用现有 ResumeList。
 */
export default function ResumeListPanel() {
  return (
    <div className="h-full overflow-auto">
      <ResumeList />
    </div>
  );
}
