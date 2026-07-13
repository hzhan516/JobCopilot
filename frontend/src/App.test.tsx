import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Outlet } from 'react-router-dom'
import App from './App'

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    BrowserRouter: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  }
})

// Mock all child components to isolate App.tsx routing logic
vi.mock('@/hooks/useAuth', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => ({ isAuthenticated: true, user: null }),
}))

vi.mock('@/components/ui/sonner', () => ({
  Toaster: ({
    position,
    duration,
    offset,
    mobileOffset,
  }: {
    position?: string
    duration?: number
    offset?: string
    mobileOffset?: string
  }) => (
    <div
      data-testid="toaster"
      data-position={position}
      data-duration={duration}
      data-offset={offset}
      data-mobile-offset={mobileOffset}
    />
  ),
}))

vi.mock('@/components/layout/MainLayout', () => ({
  default: () => (
    <div data-testid="main-layout">
      <Outlet />
    </div>
  ),
}))

vi.mock('@/components/layout/ErrorBoundary', () => ({
  default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

vi.mock('@/components/ProtectedRoute', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="protected-route">{children}</div>
  ),
}))

vi.mock('@/components/PublicRoute', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="public-route">{children}</div>
  ),
}))

vi.mock('@/pages/auth/Login', () => ({
  default: () => <div data-testid="login-page">Login</div>,
}))

vi.mock('@/pages/auth/Register', () => ({
  default: () => <div data-testid="register-page">Register</div>,
}))

vi.mock('@/pages/Dashboard', () => ({
  default: () => <div data-testid="dashboard-page">Dashboard</div>,
}))

vi.mock('@/pages/resumes/ResumesPage', () => ({
  default: () => <div data-testid="resumes-page">ResumesPage</div>,
}))

vi.mock('@/pages/resumes/ResumeEdit', () => ({
  default: () => <div data-testid="resume-edit-page">ResumeEdit</div>,
}))

vi.mock('@/pages/jobs/JobsPage', () => ({
  default: () => <div data-testid="jobs-page">JobsPage</div>,
}))

vi.mock('@/pages/tracking/TrackingPage', () => ({
  default: () => <div data-testid="tracking-page">TrackingPage</div>,
}))

vi.mock('@/pages/profile/Profile', () => ({
  default: () => <div data-testid="profile-page">Profile</div>,
}))

describe('App routing', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login page on /login', () => {
    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('public-route')).toBeInTheDocument()
    expect(screen.getByTestId('login-page')).toBeInTheDocument()
  })

  it('renders register page on /register', () => {
    render(
      <MemoryRouter initialEntries={['/register']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('public-route')).toBeInTheDocument()
    expect(screen.getByTestId('register-page')).toBeInTheDocument()
  })

  it('renders dashboard on / with ProtectedRoute', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('protected-route')).toBeInTheDocument()
    expect(screen.getByTestId('main-layout')).toBeInTheDocument()
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument()
  })

  it('renders resumes page on /resumes', () => {
    render(
      <MemoryRouter initialEntries={['/resumes']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resumes-page')).toBeInTheDocument()
  })

  it('renders resumes page on /resumes/:groupId', () => {
    render(
      <MemoryRouter initialEntries={['/resumes/group-123']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resumes-page')).toBeInTheDocument()
  })

  it('renders resume edit on /resumes/:groupId/versions/:versionId/edit', () => {
    render(
      <MemoryRouter initialEntries={['/resumes/group-123/versions/v1/edit']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resume-edit-page')).toBeInTheDocument()
  })

  it('renders jobs page on /jobs', () => {
    render(
      <MemoryRouter initialEntries={['/jobs']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('jobs-page')).toBeInTheDocument()
  })

  it('renders jobs page on /jobs/:jobId', () => {
    render(
      <MemoryRouter initialEntries={['/jobs/job-456']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('jobs-page')).toBeInTheDocument()
  })

  it('redirects the legacy /chat route to the workspace after opening Copilot', () => {
    render(
      <MemoryRouter initialEntries={['/chat']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument()
  })

  it('renders tracking page on /applications', () => {
    render(
      <MemoryRouter initialEntries={['/applications']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('tracking-page')).toBeInTheDocument()
  })

  it('renders profile on /profile', () => {
    render(
      <MemoryRouter initialEntries={['/profile']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('profile-page')).toBeInTheDocument()
  })

  it('redirects unknown paths to /', () => {
    render(
      <MemoryRouter initialEntries={['/unknown-path']}>
        <App />
      </MemoryRouter>
    )
    // Navigate should redirect to / — dashboard should render
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument()
  })

  it('renders Toaster component globally', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('toaster')).toBeInTheDocument()
  })

  it('configures global toast placement and duration', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    )

    const toaster = screen.getByTestId('toaster')
    expect(toaster).toHaveAttribute('data-position', 'top-center')
    expect(toaster).toHaveAttribute('data-duration', '6000')
    expect(toaster).toHaveAttribute('data-offset', '72px')
    expect(toaster).toHaveAttribute('data-mobile-offset', '72px')
  })
})
