import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Group, Panel, Separator, usePanelRef } from 'react-resizable-panels';
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
  const { t } = useTranslation();
  const location = useLocation();
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
  const copilotPanelRef = usePanelRef();
  const isDashboard = location.pathname === '/';
  const usesResizableCopilot = usesThreeColumnLayout && !isDashboard && location.pathname !== '/chat';

  useEffect(() => {
    if (!usesResizableCopilot || !copilotPanelRef.current) return;
    copilotPanelRef.current.resize(railCollapsed ? '56px' : '29.4118%');
  }, [copilotPanelRef, railCollapsed, usesResizableCopilot]);

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

  const desktopColumns = collapsed
    ? '68px minmax(0, 1fr)'
    : '15% minmax(0, 85%)';

  const workspace = (
    <div data-testid="main-workspace-region" className="flex flex-col flex-1 min-w-0 min-h-0">
      {usesSheetNavigation && <MinimalHeader />}

      <main className="flex-1 min-h-0 min-w-0 overflow-hidden">
        <div data-testid="main-workspace-scroll" className="h-full min-w-0 overflow-y-auto px-4 py-6">
          <Outlet />
        </div>
      </main>
    </div>
  );

  return (
    <div
      data-testid="app-shell"
      data-layout={usesThreeColumnLayout
        ? isDashboard
          ? 'dashboard'
          : 'resizable-three-column'
        : 'compact'}
      data-layout-ratio={usesThreeColumnLayout && !collapsed
        ? isDashboard
          ? '1.5:8.5'
          : usesResizableCopilot && !railCollapsed
            ? '1.5:6:2.5'
            : undefined
        : undefined}
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

      {usesResizableCopilot ? (
        <Group
          id="workspace-copilot-group"
          orientation="horizontal"
          className="h-full min-w-0"
          data-testid="workspace-copilot-group"
        >
          <Panel
            id="workspace-panel"
            defaultSize="70.5882%"
            minSize="560px"
            className="h-full min-w-0 overflow-hidden"
          >
            {workspace}
          </Panel>

          <Separator
            id="workspace-copilot-separator"
            data-testid="workspace-copilot-separator"
            disabled={railCollapsed}
            aria-label={t('layout.sidebar.copilot.resize')}
            className="group relative w-1.5 shrink-0 cursor-col-resize bg-border transition-colors hover:bg-blue-400 focus-visible:bg-blue-500 focus-visible:outline-none data-[disabled]:cursor-default data-[disabled]:bg-border active:bg-blue-500"
          >
            <span className="absolute inset-y-0 left-1/2 w-px -translate-x-1/2 bg-border group-hover:bg-blue-500" />
          </Separator>

          <Panel
            id="copilot-panel"
            panelRef={copilotPanelRef}
            defaultSize="29.4118%"
            minSize={railCollapsed ? '56px' : '320px'}
            maxSize={railCollapsed ? '56px' : '50%'}
            disabled={railCollapsed}
            className="h-full min-w-0 overflow-hidden"
          >
            <CopilotRail />
          </Panel>
        </Group>
      ) : (
        workspace
      )}

      {/* Dashboard 不占用常驻 Chat 列；窄屏与 Dashboard 按需使用覆盖式 Drawer。 */}
      {!usesResizableCopilot && <GlobalCopilotDrawer />}
    </div>
  );
}
