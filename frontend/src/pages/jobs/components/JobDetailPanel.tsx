import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCopilotStore } from '@/store/copilot.store';
import JobDetail from '@/pages/jobs/JobDetail';
import { Button } from '@/components/ui/button';
import { MessageSquare } from 'lucide-react';

/**
 * Master-Detail 右侧面板 — 包裹现有 JobDetail，添加 "Ask Copilot" 入口。
 * 当从 Job 详情唤出 Copilot 时自动带入当前 Job context。
 */
export default function JobDetailPanel() {
  const { t } = useTranslation();
  const { jobId } = useParams<{ jobId: string }>();
  const { open: openCopilot } = useCopilotStore();

  return (
    <div className="h-full overflow-auto">
      {/* "Ask Copilot" 浮动按钮 */}
      {jobId && (
        <div className="sticky top-0 z-10 flex justify-end px-4 py-2 bg-background/80 backdrop-blur border-b">
          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              openCopilot({ type: 'job', id: jobId, title: '' })
            }
          >
            <MessageSquare className="w-4 h-4 mr-2" />
            {t('layout.sidebar.copilot.openCopilot')}
          </Button>
        </div>
      )}
      <JobDetail />
    </div>
  );
}
