import ResumeDetail from '@/pages/resumes/ResumeDetail';

/**
 * Master-Detail 右侧面板 — 包裹现有 ResumeDetail。
 * 注意：ResumeDetail 内部已是分栏布局，此处仅作面板包裹。
 */
export default function ResumeDetailPanel() {
  return (
    <div className="h-full min-w-0 overflow-x-hidden overflow-y-auto">
      <ResumeDetail />
    </div>
  );
}
