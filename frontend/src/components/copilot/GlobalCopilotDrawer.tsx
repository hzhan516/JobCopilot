import { useTranslation } from 'react-i18next';
import { useCopilotStore } from '@/store/copilot.store';
import { Sheet, SheetContent } from '@/components/ui/sheet';
import CopilotChatArea from '@/components/copilot/CopilotChatArea';

/**
 * 全局 AI Copilot Drawer — 右侧滑出面板。
 * 挂载在 MainLayout 中，各页面通过 copilot.store 控制开关。
 */
export default function GlobalCopilotDrawer() {
  const { t } = useTranslation();
  const { isOpen, close } = useCopilotStore();

  return (
    <Sheet open={isOpen} onOpenChange={(open) => { if (!open) close(); }}>
      <SheetContent
        side="right"
        className="w-[90vw] max-w-[480px] sm:max-w-[540px] p-0 flex flex-col"
      >
        {/* 无障碍标题 */}
        <span className="sr-only">{t('layout.sidebar.copilot.openCopilot')}</span>
        <CopilotChatArea />
      </SheetContent>
    </Sheet>
  );
}
