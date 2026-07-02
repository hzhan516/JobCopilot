import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { useIsMobile } from '@/hooks/use-mobile';
import { useSidebarStore } from '@/store/sidebar.store';
import { useCopilotStore } from '@/store/copilot.store';
import AppSidebar from '@/components/layout/AppSidebar';
import MinimalHeader from '@/components/layout/MinimalHeader';
import GlobalCopilotDrawer from '@/components/copilot/GlobalCopilotDrawer';
import { Sheet, SheetContent } from '@/components/ui/sheet';

export default function MainLayout() {
  const { isAuthenticated } = useAuth();
  const isMobile = useIsMobile();
  const { mobileOpen, setMobileOpen, toggle } = useSidebarStore();
  const { open: openCopilot, isOpen: copilotOpen, close: closeCopilot } = useCopilotStore();

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

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* 桌面侧边栏 — 固定左侧 */}
      {!isMobile && <AppSidebar />}

      {/* 移动端侧边栏 — Sheet 包裹 */}
      {isMobile && (
        <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
          <SheetContent side="left" className="w-60 p-0">
            <AppSidebar isMobile onNavigate={() => setMobileOpen(false)} />
          </SheetContent>
        </Sheet>
      )}

      {/* 右侧主区域 */}
      <div className="flex flex-col flex-1 min-w-0">
        {/* 移动端顶部栏 */}
        {isMobile && <MinimalHeader />}

        {/* 页面内容 — Outlet 渲染子路由 */}
        <main className="flex-1 overflow-auto">
          <div className="container mx-auto px-4 py-6">
            <Outlet />
          </div>
        </main>
      </div>

      {/* 全局 AI Copilot Drawer — 所有页面可唤出 */}
      <GlobalCopilotDrawer />
    </div>
  );
}
