import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useCopilotStore } from '@/store/copilot.store';
import { MessageSquare, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

/**
 * /chat 路由页面 — 不再承载完整聊天 UI。
 * 访问时自动打开全局 Copilot Drawer，页面本身作为引导占位。
 */
export default function Chat() {
  const { t } = useTranslation();
  const { open } = useCopilotStore();

  // 访问时自动打开 Drawer
  useEffect(() => {
    open({ type: 'general' });
  }, [open]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center">
      <div className="w-16 h-16 bg-purple-100 dark:bg-purple-900/30 rounded-full flex items-center justify-center mb-6">
        <MessageSquare className="w-8 h-8 text-purple-600 dark:text-purple-400" />
      </div>

      <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-2">
        {t('layout.nav.chat')}
      </h2>
      <p className="text-muted-foreground max-w-md mb-6">
        {t('chat.drawerRedirect')}
      </p>

      <Button variant="outline" onClick={() => open({ type: 'general' })}>
        <MessageSquare className="w-4 h-4 mr-2" />
        {t('layout.sidebar.copilot.openCopilot')}
        <ArrowRight className="w-4 h-4 ml-2" />
      </Button>
    </div>
  );
}
