import { useParams } from 'react-router-dom';
import { useMasterDetail } from '@/hooks/useMediaQuery';
import ResumeList from '@/pages/resumes/ResumeList';
import ResumeDetail from '@/pages/resumes/ResumeDetail';
import ResumeListPanel from '@/pages/resumes/components/ResumeListPanel';
import ResumeDetailPanel from '@/pages/resumes/components/ResumeDetailPanel';
import { Panel, Group, Separator } from 'react-resizable-panels';
import { FileText } from 'lucide-react';

/**
 * Resumes Master-Detail 页面容器。
 * ≥1024px 分栏：左侧 ResumeList + 右侧 ResumeDetail
 * <1024px  回退路由跳转：/resumes 显示列表，/resumes/:groupId 显示详情
 */
export default function ResumesPage() {
  const isWide = useMasterDetail();
  const { groupId } = useParams<{ groupId: string }>();

  // 窄屏模式：零修改复用现有全屏组件
  if (!isWide) {
    if (groupId) return <ResumeDetail />;
    return <ResumeList />;
  }

  // 宽屏 Master-Detail 模式
  return (
    <div className="-mx-4 -my-6 h-[calc(100vh-3.5rem)]">
      <Group orientation="horizontal" id="resumes-master-detail">
        {/* 左侧列表 */}
        <Panel defaultSize={35} minSize={28} maxSize={45}>
          <ResumeListPanel />
        </Panel>

        <Separator className="w-1.5 bg-border hover:bg-blue-400 transition-colors active:bg-blue-500" />

        {/* 右侧详情 */}
        <Panel defaultSize={65} minSize={40}>
          {groupId ? (
            <ResumeDetailPanel />
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-muted-foreground bg-muted/20">
              <FileText className="w-16 h-16 mb-4 opacity-30" />
              <p className="text-lg font-medium">Select a resume</p>
              <p className="text-sm mt-1">Choose a resume from the list to view details</p>
            </div>
          )}
        </Panel>
      </Group>
    </div>
  );
}
