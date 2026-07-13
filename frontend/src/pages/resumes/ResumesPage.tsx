import { useParams } from 'react-router-dom';
import ResumeListPanel from '@/pages/resumes/components/ResumeListPanel';
import ResumeDetailPanel from '@/pages/resumes/components/ResumeDetailPanel';

/**
 * 中央工作区使用路由切换列表/详情，不再在 5fr 内容栏内二次横向切分。
 */
export default function ResumesPage() {
  const { groupId } = useParams<{ groupId: string }>();

  return (
    <div data-testid="resumes-route-layout" className="h-full min-w-0">
      {groupId ? <ResumeDetailPanel /> : <ResumeListPanel />}
    </div>
  );
}
