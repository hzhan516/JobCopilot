import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AppSidebar from './AppSidebar';

const longEmail = 'a-very-long-user-name-that-must-stay-inside-the-sidebar@example.com';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    user: { email: longEmail },
    logout: vi.fn(),
  }),
}));

vi.mock('@/store/sidebar.store', () => ({
  useSidebarStore: () => ({ collapsed: false, toggle: vi.fn() }),
}));

vi.mock('@/store/copilot.store', () => ({
  useCopilotStore: () => ({ open: vi.fn() }),
}));

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <button data-testid="language-switcher">Language</button>,
}));

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, className, onClick }: any) => (
    <button className={className} onClick={onClick}>{children}</button>
  ),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <>{children}</>,
  DropdownMenuTrigger: ({ children }: any) => children,
  DropdownMenuContent: ({ children }: any) => <div>{children}</div>,
  DropdownMenuItem: ({ children }: any) => <div>{children}</div>,
  DropdownMenuSeparator: () => <hr />,
}));

vi.mock('lucide-react', () => ({
  FileText: () => <span>file</span>,
  User: () => <span>user</span>,
  LogOut: () => <span>logout</span>,
  ChevronDown: () => <span>down</span>,
  ChevronLeft: () => <span>left</span>,
  ChevronRight: () => <span>right</span>,
  MessageSquare: () => <span>chat</span>,
  Briefcase: () => <span>job</span>,
  ClipboardList: () => <span>tracking</span>,
  LayoutDashboard: () => <span>dashboard</span>,
}));

describe('AppSidebar user identity layout', () => {
  it('truncates a long user name without allowing the trigger to overflow', () => {
    render(
      <MemoryRouter>
        <AppSidebar fillAvailableWidth />
      </MemoryRouter>,
    );

    const userName = screen.getByText(longEmail);
    expect(userName).toHaveClass('min-w-0', 'flex-1', 'truncate');

    const trigger = userName.closest('button');
    expect(trigger).not.toBeNull();
    expect(trigger).toHaveClass('min-w-0', 'max-w-full', 'overflow-hidden', 'flex-1');
    expect(trigger?.parentElement).toHaveClass('min-w-0');
  });
});
