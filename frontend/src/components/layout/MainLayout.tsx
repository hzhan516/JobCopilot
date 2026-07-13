import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { useSidebarStore } from '@/store/sidebar.store';
import { useCopilotStore } from '@/store/copilot.store';
import AppSidebar from '@/components/layout/AppSidebar';
import MinimalHeader from '@/components/layout/MinimalHeader';
import GlobalCopilotDrawer from '@/components/copilot/GlobalCopilotDrawer';
import CopilotRail from '@/components/copilot/CopilotRail';
import { Sheet, SheetContent } from '@/components/ui/sheet';

export default function MainLayout() {
  const { isAuthenticated } = useAuth();
  const usesSheetNavigation = useMediaQuery('(max-width: 1023px)');
  const usesThreeColumnLayout = useMediaQuery('(min-width: 1280px)');
  const { collapsed, mobileOpen, setMobileOpen, toggle } = useSidebarStore();
  const {
    open: openCopilot,
    isOpen: copilotOpen,
    close: closeCopilot,
    railCollapsed,
  } = useCopilotStore();

  // 全局快捷键
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const mod = e.metaKey || e.ctrlKey;
      if (!mod) return;

      // Cmd/Ctrl + . → 切换 Copilot Drawer
      if (e.key === '.') {
        e.preventDefault();
        if (copilotOpen) {
          closeCopilot();
        } else {
          openCopilot({ type: 'general' });
        }
      }

      // Cmd/Ctrl + B → 切换侧边栏收起/展开
      if (e.key === 'b' || e.key === 'B') {
        e.preventDefault();
        toggle();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [copilotOpen, openCopilot, closeCopilot, toggle]);

  // 未认证时不做布局包裹（保留原行为）
  if (!isAuthenticated) {
    return <Outlet />;
  }

  const desktopColumns = railCollapsed
    ? collapsed
      ? '68px minmax(560px, 1fr) 56px'
      : 'minmax(220px, 20vw) minmax(560px, 1fr) 56px'
    : collapsed
      ? '68px minmax(560px, 5fr) minmax(320px, 3fr)'
      : 'minmax(220px, 2fr) minmax(560px, 5fr) minmax(320px, 3fr)';

  return (
    <div
      data-testid="app-shell"
      data-layout={usesThreeColumnLayout ? 'three-column' : 'compact'}
      data-layout-ratio={usesThreeColumnLayout && !railCollapsed && !collapsed ? '2:5:3' : undefined}
      className={`${usesThreeColumnLayout ? 'grid' : 'flex'} h-screen overflow-hidden bg-background`}
      style={usesThreeColumnLayout ? { gridTemplateColumns: desktopColumns } : undefined}
    >
      {/* 桌面侧边栏 — 固定左侧 */}
      {!usesSheetNavigation && (
        <AppSidebar
          forceCollapsed={!usesThreeColumnLayout}
          fillAvailableWidth={usesThreeColumnLayout}
        />
      )}

      {/* 移动端侧边栏 — Sheet 包裹 */}
      {usesSheetNavigation && (
        <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
          <SheetContent side="left" className="w-60 p-0">
            <AppSidebar isMobile onNavigate={() => setMobileOpen(false)} />
          </SheetContent>
        </Sheet>
      )}

      {/* 右侧主区域 */}
      <div data-testid="main-workspace-region" className="flex flex-col flex-1 min-w-0 min-h-0">
        {/* 移动端顶部栏 */}
        {usesSheetNavigation && <MinimalHeader />}

        {/* 页面内容 — Outlet 渲染子路由 */}
        <main className="flex-1 min-h-0 min-w-0 overflow-hidden">
          <div data-testid="main-workspace-scroll" className="h-full min-w-0 overflow-y-auto px-4 py-6">
            <Outlet />
          </div>
        </main>
      </div>

      {/* 宽屏常驻 AI Rail；窄屏继续使用覆盖式 Drawer。 */}
      {usesThreeColumnLayout ? <CopilotRail /> : <GlobalCopilotDrawer />}
    </div>
  );
}
