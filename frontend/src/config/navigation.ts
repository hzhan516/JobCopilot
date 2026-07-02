import { FileText, Briefcase, MessageSquare, ClipboardList, LayoutDashboard } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export interface NavItem {
  path: string;
  labelKey: string;
  icon: LucideIcon;
}

/**
 * 主侧边栏导航项。
 * labelKey 对应 i18n locales 中的 layout.nav.* 系列 key。
 */
export const mainNavItems: NavItem[] = [
  { path: '/', labelKey: 'layout.nav.dashboard', icon: LayoutDashboard },
  { path: '/resumes', labelKey: 'layout.nav.resumes', icon: FileText },
  { path: '/jobs', labelKey: 'layout.nav.jobs', icon: Briefcase },
  { path: '/applications', labelKey: 'layout.nav.tracking', icon: ClipboardList },
];

/**
 * Chat 入口独立管理（在 sidebar 底部以特殊按钮展示，不使用 NavLink）。
 * 阶段二中将被重定向为打开全局 Copilot Drawer。
 */
export const chatNavItem: NavItem = {
  path: '/chat',
  labelKey: 'layout.nav.chat',
  icon: MessageSquare,
};
