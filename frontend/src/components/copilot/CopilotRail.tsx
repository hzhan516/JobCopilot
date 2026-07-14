import { useTranslation } from 'react-i18next';
import { MessageSquare, PanelRightClose, PanelRightOpen } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useCopilotStore } from '@/store/copilot.store';
import CopilotChatArea from '@/components/copilot/CopilotChatArea';

/** 桌面端常驻 AI Chat 区域；窄屏继续由 GlobalCopilotDrawer 承载。 */
export default function CopilotRail() {
  const { t } = useTranslation();
  const { railCollapsed, toggleRail } = useCopilotStore();

  if (railCollapsed) {
    return (
      <aside
        data-testid="copilot-rail-region"
        data-collapsed="true"
        className="h-full min-w-0 bg-background flex flex-col items-center py-3"
      >
        <Button
          variant="ghost"
          size="icon"
          onClick={toggleRail}
          aria-label={t('layout.sidebar.copilot.expand')}
        >
          <PanelRightOpen className="w-5 h-5" />
        </Button>
        <MessageSquare className="w-5 h-5 mt-3 text-muted-foreground" aria-hidden="true" />
        <span className="sr-only">{t('layout.nav.chat')}</span>
      </aside>
    );
  }

  return (
    <aside
      data-testid="copilot-rail-region"
      data-collapsed="false"
      className="h-full min-w-0 bg-background flex flex-col overflow-hidden"
    >
      <div className="h-14 px-4 border-b flex items-center justify-between shrink-0">
        <div className="min-w-0 flex items-center gap-2 font-semibold">
          <MessageSquare className="w-5 h-5 shrink-0" aria-hidden="true" />
          <span className="truncate">{t('layout.nav.chat')}</span>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={toggleRail}
          aria-label={t('layout.sidebar.copilot.collapse')}
        >
          <PanelRightClose className="w-5 h-5" />
        </Button>
      </div>
      <div className="flex-1 min-h-0 min-w-0">
        <CopilotChatArea />
      </div>
    </aside>
  );
}
