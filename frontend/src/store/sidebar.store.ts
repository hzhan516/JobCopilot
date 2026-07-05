import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SidebarState {
  /** 桌面端 icon-only 模式，偏好写入 localStorage */
  collapsed: boolean;
  /** 移动端 Sheet 开关，不持久化 */
  mobileOpen: boolean;
  toggle: () => void;
  setMobileOpen: (open: boolean) => void;
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set) => ({
      collapsed: false,
      mobileOpen: false,
      toggle: () => set((s) => ({ collapsed: !s.collapsed })),
      setMobileOpen: (open) => set({ mobileOpen: open }),
    }),
    {
      name: 'jobcopilot-sidebar',
      // 仅持久化 collapsed，mobileOpen 属于瞬时 UI 状态
      partialize: (state) => ({ collapsed: state.collapsed }),
    }
  )
);
