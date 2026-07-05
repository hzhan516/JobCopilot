import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSidebarStore } from '@/store/sidebar.store';
import { useCopilotStore } from '@/store/copilot.store';
import { Button } from '@/components/ui/button';
import { Menu, MessageSquare } from 'lucide-react';

/** 路径 → i18n key 的页面标题映射 */
const PAGE_TITLE_MAP: Record<string, string> = {
  '/': 'layout.nav.dashboard',
  '/resumes': 'layout.nav.resumes',
  '/jobs': 'layout.nav.jobs',
  '/chat': 'layout.nav.chat',
  '/applications': 'layout.nav.tracking',
  '/profile': 'layout.userMenu.profile',
};

function getPageTitle(pathname: string): string {
  // 精确匹配优先，然后前缀匹配
  if (PAGE_TITLE_MAP[pathname]) return PAGE_TITLE_MAP[pathname];
  for (const [prefix, key] of Object.entries(PAGE_TITLE_MAP)) {
    if (prefix !== '/' && pathname.startsWith(prefix)) return key;
  }
  return 'layout.nav.dashboard';
}

export default function MinimalHeader() {
  const { t } = useTranslation();
  const location = useLocation();
  const { setMobileOpen } = useSidebarStore();
  const { open: openCopilot } = useCopilotStore();

  const pageTitle = t(getPageTitle(location.pathname));

  return (
    <header className="sticky top-0 z-40 h-14 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 flex items-center justify-between px-4 lg:hidden">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setMobileOpen(true)}
          aria-label={t('layout.sidebar.toggle')}
        >
          <Menu className="w-5 h-5" />
        </Button>
        <h1 className="text-sm font-semibold truncate">{pageTitle}</h1>
      </div>

      {/* AI Copilot Drawer 开关 */}
      <Button
        variant="ghost"
        size="icon"
        onClick={() => openCopilot({ type: 'general' })}
        aria-label={t('layout.sidebar.copilot.openCopilot')}
      >
        <MessageSquare className="w-5 h-5" />
      </Button>
    </header>
  );
}
