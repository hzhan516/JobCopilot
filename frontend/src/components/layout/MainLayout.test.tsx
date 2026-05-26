import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MainLayout from './MainLayout'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useLocation: () => ({ pathname: '/resumes' }),
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockLogout = vi.fn()
let mockAuth = {
  user: { email: 'test@example.com', name: 'Test User' },
  logout: mockLogout,
  isAuthenticated: true,
}

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockAuth,
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, variant, size, className, asChild }: any) => {
    if (asChild) return <a className={className}>{children}</a>
    return <button onClick={onClick} className={className}>{children}</button>
  },
}))

vi.mock('@/components/ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children, asChild }: any) => children,
  DropdownMenuContent: ({ children, align, className }: any) => <div className={className}>{children}</div>,
  DropdownMenuItem: ({ children, onClick, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
  DropdownMenuSeparator: () => <hr />,
}))

vi.mock('@/components/ui/sheet', () => ({
  Sheet: ({ children }: any) => <div>{children}</div>,
  SheetTrigger: ({ children, asChild, className }: any) => children,
  SheetContent: ({ children, side, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <div data-testid="language-switcher">Lang</div>,
}))

vi.mock('lucide-react', () => ({
  FileText: () => <span>📄</span>,
  Briefcase: () => <span>💼</span>,
  MessageSquare: () => <span>💬</span>,
  ClipboardList: () => <span>📋</span>,
  User: () => <span>👤</span>,
  LogOut: () => <span>🚪</span>,
  Menu: () => <span>☰</span>,
  ChevronDown: () => <span>▼</span>,
}))

describe('MainLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockAuth = {
      user: { email: 'test@example.com', name: 'Test User' },
      logout: mockLogout,
      isAuthenticated: true,
    }
  })

  it('renders header with app name and navigation', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div data-testid="page-content">Page Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByText('common.appName')).toBeInTheDocument()
    expect(screen.getByText('layout.nav.resumes')).toBeInTheDocument()
    expect(screen.getByText('layout.nav.jobs')).toBeInTheDocument()
    expect(screen.getByText('layout.nav.chat')).toBeInTheDocument()
    expect(screen.getByText('layout.nav.tracking')).toBeInTheDocument()
  })

  it('renders user email in dropdown', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByText('test@example.com')).toBeInTheDocument()
  })

  it('renders language switcher', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByTestId('language-switcher')).toBeInTheDocument()
  })

  it('renders children content', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div data-testid="page-content">Page Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByTestId('page-content')).toBeInTheDocument()
  })

  it('renders mobile menu button', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByText('☰')).toBeInTheDocument()
  })

  it('renders profile menu item', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByText('layout.userMenu.profile')).toBeInTheDocument()
  })

  it('handles logout and navigates to login', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('layout.userMenu.logout'))

    expect(mockLogout).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/login')
  })

  it('renders without layout wrapper when not authenticated', () => {
    mockAuth = { user: null, logout: mockLogout, isAuthenticated: false }

    render(
      <MemoryRouter>
        <MainLayout>
          <div data-testid="page-content">Public Page</div>
        </MainLayout>
      </MemoryRouter>
    )

    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.queryByText('common.appName')).not.toBeInTheDocument()
  })

  it('renders active nav item with different style', () => {
    vi.mock('react-router-dom', async () => {
      const actual = await vi.importActual('react-router-dom')
      return {
        ...actual,
        useLocation: () => ({ pathname: '/jobs' }),
        useNavigate: () => mockNavigate,
      }
    })

    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    // The active item should have a different class
    expect(screen.getByText('layout.nav.jobs')).toBeInTheDocument()
  })

  it('navigates to profile on profile menu click', () => {
    render(
      <MemoryRouter>
        <MainLayout>
          <div>Content</div>
        </MainLayout>
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('layout.userMenu.profile'))
    expect(mockNavigate).toHaveBeenCalledWith('/profile')
  })
})
