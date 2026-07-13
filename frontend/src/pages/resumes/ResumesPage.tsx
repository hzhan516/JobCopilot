import { useParams } from 'react-router-dom';
import ResumeList from '@/pages/resumes/ResumeList';
import ResumeDetail from '@/pages/resumes/ResumeDetail';
import ResumeListPanel from '@/pages/resumes/components/ResumeListPanel';
import ResumeDetailPanel from '@/pages/resumes/components/ResumeDetailPanel';
import MasterDetailLayout from '@/components/layout/MasterDetailLayout';
import MasterDetailEmpty from '@/components/layout/MasterDetailEmpty';
import { FileText } from 'lucide-react';

/**
 * Resumes Master-Detail 页面容器。
 * 宽屏分栏：左侧 ResumeList + 右侧 ResumeDetail
 * 窄屏回退：/resumes 显示列表，/resumes/:groupId 显示详情
 *
 * 是否分栏由 MasterDetailLayout 依据实际可用内容宽度决定（而非视口宽度）。
 */
export default function ResumesPage() {
  const { groupId } = useParams<{ groupId: string }>();

  return (
    <MasterDetailLayout
      groupId="resumes-master-detail"
      defaultListPercent={35}
      hasSelection={!!groupId}
      list={<ResumeListPanel />}
      detail={<ResumeDetailPanel />}
      emptyDetail={
        <MasterDetailEmpty
          icon={FileText}
          titleKey="masterDetail.resume.emptyTitle"
          descKey="masterDetail.resume.emptyDesc"
        />
      }
      narrowList={<ResumeList />}
      narrowDetail={<ResumeDetail />}
    />
  );
}
