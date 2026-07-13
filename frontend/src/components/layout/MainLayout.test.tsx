import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MainLayout from './MainLayout'

const mockNavigate = vi.fn()
const mockLogout = vi.fn()
let mockAuth = {
  user: { email: 'test@example.com', name: 'Test User' } as any,
  logout: mockLogout,
  isAuthenticated: true,
}
let mockUsesSheetNavigation = false
let mockUsesThreeColumnLayout = true
let mockRailCollapsed = false
const mockPanelResize = vi.fn()

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockAuth,
}))

vi.mock('@/hooks/useMediaQuery', () => ({
  useMediaQuery: (query: string) => query.includes('min-width')
    ? mockUsesThreeColumnLayout
    : mockUsesSheetNavigation,
}))

vi.mock('@/store/sidebar.store', () => ({
  useSidebarStore: () => ({
    collapsed: false,
    mobileOpen: false,
    toggle: vi.fn(),
    setMobileOpen: vi.fn(),
  }),
}))

vi.mock('@/store/copilot.store', () => ({
  useCopilotStore: () => ({
    isOpen: false,
    railCollapsed: mockRailCollapsed,
    open: vi.fn(),
    close: vi.fn(),
  }),
}))

vi.mock('react-resizable-panels', () => ({
  Group: ({ children, id, ...props }: any) => (
    <div data-testid={props['data-testid'] ?? id}>{children}</div>
  ),
  Panel: ({ children, id, defaultSize, minSize, maxSize, disabled }: any) => (
    <div
      data-testid={id}
      data-default-size={defaultSize}
      data-min-size={minSize}
      data-max-size={maxSize}
      data-disabled={String(!!disabled)}
    >
      {children}
    </div>
  ),
  Separator: ({ children, id, disabled, 'aria-label': ariaLabel }: any) => (
    <div
      data-testid={id}
      data-disabled={String(!!disabled)}
      aria-label={ariaLabel}
    >
      {children}
    </div>
  ),
  usePanelRef: () => ({ current: { resize: mockPanelResize } }),
}))

vi.mock('@/components/layout/AppSidebar', () => ({
  default: ({ isMobile, onNavigate }: any) => (
    <div data-testid="app-sidebar" data-mobile={String(!!isMobile)}>
      Sidebar
    </div>
  ),
}))

vi.mock('@/components/layout/MinimalHeader', () => ({
  default: () => <div data-testid="minimal-header">Header</div>,
}))

vi.mock('@/components/copilot/GlobalCopilotDrawer', () => ({
  default: () => <div data-testid="global-copilot-drawer">Drawer</div>,
}))

vi.mock('@/components/copilot/CopilotRail', () => ({
  default: () => <div data-testid="copilot-rail-region">Rail</div>,
}))

vi.mock('@/components/ui/sheet', () => ({
  Sheet: ({ children }: any) => <div>{children}</div>,
  SheetContent: ({ children }: any) => <div>{children}</div>,
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
  ChevronLeft: () => <span>◀</span>,
  ChevronRight: () => <span>▶</span>,
  LayoutDashboard: () => <span>🏠</span>,
}))

function renderWithRoute(initialRoute = '/') {
  return render(
    <MemoryRouter initialEntries={[initialRoute]}>
      <Routes>
        <Route element={<MainLayout />}>
          <Route index element={<div data-testid="page-content">Dashboard Page</div>} />
          <Route path="/resumes" element={<div data-testid="page-content">Resumes Page</div>} />
          <Route path="/profile" element={<div data-testid="page-content">Profile Page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>
  )
}

describe('MainLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockAuth = {
      user: { email: 'test@example.com', name: 'Test User' },
      logout: mockLogout,
      isAuthenticated: true,
    }
    mockUsesSheetNavigation = false
    mockUsesThreeColumnLayout = true
    mockRailCollapsed = false
  })

  it('renders AppSidebar on desktop', () => {
    renderWithRoute('/')
    expect(screen.getByTestId('app-sidebar')).toBeInTheDocument()
    // Desktop sidebar should not have mobile flag
    expect(screen.getByTestId('app-sidebar')).toHaveAttribute('data-mobile', 'false')
  })

  it('keeps Dashboard free of the persistent Copilot rail on wide desktop', () => {
    renderWithRoute('/')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout', 'dashboard')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout-ratio', '1.5:8.5')
    expect(screen.queryByTestId('copilot-rail-region')).not.toBeInTheDocument()
    expect(screen.queryByTestId('workspace-copilot-separator')).not.toBeInTheDocument()
    expect(screen.getByTestId('global-copilot-drawer')).toBeInTheDocument()
  })

  it('uses a resizable 1.5:6:2.5 layout on wide business pages', () => {
    renderWithRoute('/resumes')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout', 'resizable-three-column')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout-ratio', '1.5:6:2.5')
    expect(screen.getByTestId('copilot-rail-region')).toBeInTheDocument()
    expect(screen.queryByTestId('global-copilot-drawer')).not.toBeInTheDocument()
    expect(screen.getByTestId('workspace-panel')).toHaveAttribute('data-default-size', '70.5882%')
    expect(screen.getByTestId('copilot-panel')).toHaveAttribute('data-default-size', '29.4118%')
    expect(screen.getByTestId('workspace-copilot-separator')).toHaveAttribute('data-disabled', 'false')
    expect(screen.getByTestId('workspace-copilot-separator')).toHaveAttribute(
      'aria-label',
      'layout.sidebar.copilot.resize',
    )
    expect(mockPanelResize).toHaveBeenCalledWith('29.4118%')
  })

  it('locks the workspace separator while the Copilot rail is collapsed', () => {
    mockRailCollapsed = true
    renderWithRoute('/resumes')
    expect(screen.getByTestId('workspace-copilot-separator')).toHaveAttribute('data-disabled', 'true')
    expect(screen.getByTestId('copilot-panel')).toHaveAttribute('data-min-size', '56px')
    expect(screen.getByTestId('copilot-panel')).toHaveAttribute('data-max-size', '56px')
    expect(mockPanelResize).toHaveBeenCalledWith('56px')
  })

  it('uses the Copilot drawer below the three-column breakpoint', () => {
    mockUsesThreeColumnLayout = false
    renderWithRoute('/resumes')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout', 'compact')
    expect(screen.getByTestId('app-shell')).not.toHaveAttribute('data-layout-ratio')
    expect(screen.getByTestId('global-copilot-drawer')).toBeInTheDocument()
    expect(screen.queryByTestId('copilot-rail-region')).not.toBeInTheDocument()
  })

  it('does not advertise the desktop Dashboard ratio below the wide breakpoint', () => {
    mockUsesThreeColumnLayout = false
    renderWithRoute('/')
    expect(screen.getByTestId('app-shell')).toHaveAttribute('data-layout', 'compact')
    expect(screen.getByTestId('app-shell')).not.toHaveAttribute('data-layout-ratio')
  })

  it('renders child route content via Outlet', () => {
    renderWithRoute('/')
    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.getByText('Dashboard Page')).toBeInTheDocument()
  })

  it('renders different page content for different routes', () => {
    renderWithRoute('/resumes')
    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.getByText('Resumes Page')).toBeInTheDocument()
  })

  it('passes content through without layout when not authenticated', () => {
    mockAuth = { user: null as any, logout: mockLogout, isAuthenticated: false }

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<MainLayout />}>
            <Route index element={<div data-testid="page-content">Public Page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByTestId('page-content')).toBeInTheDocument()
    expect(screen.queryByTestId('app-sidebar')).not.toBeInTheDocument()
    expect(screen.queryByTestId('global-copilot-drawer')).not.toBeInTheDocument()
    expect(screen.queryByTestId('copilot-rail-region')).not.toBeInTheDocument()
  })
})
