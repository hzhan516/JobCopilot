import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/hooks/useAuth';
import { useSidebarStore } from '@/store/sidebar.store';
import { useCopilotStore } from '@/store/copilot.store';
import { mainNavItems, chatNavItem } from '@/config/navigation';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { LanguageSwitcher } from '@/components/LanguageSwitcher';
import {
  FileText,
  User,
  LogOut,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  MessageSquare,
} from 'lucide-react';

interface AppSidebarProps {
  /** 移动端 Sheet 内渲染时传递，关闭回调由 Sheet 自身处理 */
  isMobile?: boolean;
  /** 移动端点击导航项后关闭 Sheet */
  onNavigate?: () => void;
}

export default function AppSidebar({ isMobile, onNavigate }: AppSidebarProps) {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { collapsed, toggle } = useSidebarStore();
  const { open: openCopilot } = useCopilotStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleChatClick = () => {
    openCopilot({ type: 'general' });
    onNavigate?.();
  };

  return (
    <aside
      className={`flex flex-col h-full bg-sidebar text-sidebar-foreground border-r border-sidebar-border shrink-0 transition-[width] duration-300 ease-in-out ${
        collapsed && !isMobile ? 'w-[68px]' : 'w-60'
      }`}
    >
      {/* Logo 区域 */}
      <div className="flex items-center h-14 px-4 border-b border-sidebar-border shrink-0">
        <Link
          to="/"
          className="flex items-center space-x-2 overflow-hidden"
          onClick={onNavigate}
        >
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center shrink-0">
            <FileText className="w-5 h-5 text-white" />
          </div>
          {(!collapsed || isMobile) && (
            <span className="text-lg font-bold truncate">{t('common.appName')}</span>
          )}
        </Link>
      </div>

      {/* 主导航 */}
      <nav className="flex-1 p-2 space-y-1 overflow-y-auto">
        {mainNavItems.map(({ path, labelKey, icon: Icon }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            onClick={onNavigate}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground'
              } ${collapsed && !isMobile ? 'justify-center px-2' : ''}`
            }
          >
            <Icon className="w-5 h-5 shrink-0" />
            {(!collapsed || isMobile) && <span>{t(labelKey)}</span>}
          </NavLink>
        ))}
      </nav>

      {/* 底部区域 */}
      <div className="border-t border-sidebar-border shrink-0">
        {/* AI Copilot 入口（阶段二中改为 Drawer 开关） */}
        <div className="p-2">
          <Button
            variant="ghost"
            className={`w-full justify-start text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground ${
              collapsed && !isMobile ? 'justify-center px-2' : ''
            }`}
            onClick={handleChatClick}
          >
            <MessageSquare className="w-5 h-5 shrink-0" />
            {(!collapsed || isMobile) && (
              <span className="ml-3">{t(chatNavItem.labelKey)}</span>
            )}
          </Button>
        </div>

        {/* 语言切换 + 用户菜单 */}
        <div
          className={`flex items-center px-3 py-2 border-t border-sidebar-border ${
            collapsed && !isMobile ? 'flex-col gap-2' : 'justify-between'
          }`}
        >
          <LanguageSwitcher />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className={`flex items-center text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground h-auto ${
                  collapsed && !isMobile ? 'justify-center p-1' : 'space-x-2 px-2 py-1'
                }`}
              >
                <div className="w-7 h-7 bg-blue-100 dark:bg-blue-900 rounded-full flex items-center justify-center shrink-0">
                  <User className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
                </div>
                {(!collapsed || isMobile) && (
                  <>
                    <span className="text-xs font-medium max-w-[100px] truncate">
                      {user?.email}
                    </span>
                    <ChevronDown className="w-3 h-3" />
                  </>
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" side={isMobile ? 'top' : 'right'} className="w-48">
              <DropdownMenuItem
                className="cursor-pointer"
                onClick={() => {
                  navigate('/profile');
                  onNavigate?.();
                }}
              >
                <User className="w-4 h-4 mr-2" />
                {t('layout.userMenu.profile')}
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="cursor-pointer text-red-600" onClick={handleLogout}>
                <LogOut className="w-4 h-4 mr-2" />
                {t('layout.userMenu.logout')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* 桌面收起按钮 */}
        {!isMobile && (
          <button
            onClick={toggle}
            className="w-full flex items-center justify-center py-2 text-sidebar-foreground/50 hover:text-sidebar-foreground hover:bg-sidebar-accent/30 transition-colors border-t border-sidebar-border"
            aria-label={collapsed ? t('layout.sidebar.expand') : t('layout.sidebar.collapse')}
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        )}
      </div>
    </aside>
  );
}
