import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
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
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="main-layout">{children}</div>
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

vi.mock('@/pages/resumes/ResumeList', () => ({
  default: () => <div data-testid="resume-list-page">ResumeList</div>,
}))

vi.mock('@/pages/resumes/ResumeDetail', () => ({
  default: () => <div data-testid="resume-detail-page">ResumeDetail</div>,
}))

vi.mock('@/pages/resumes/ResumeEdit', () => ({
  default: () => <div data-testid="resume-edit-page">ResumeEdit</div>,
}))

vi.mock('@/pages/jobs/JobList', () => ({
  default: () => <div data-testid="job-list-page">JobList</div>,
}))

vi.mock('@/pages/jobs/JobDetail', () => ({
  default: () => <div data-testid="job-detail-page">JobDetail</div>,
}))

vi.mock('@/pages/chat/Chat', () => ({
  default: () => <div data-testid="chat-page">Chat</div>,
}))

vi.mock('@/pages/tracking/Tracking', () => ({
  default: () => <div data-testid="tracking-page">Tracking</div>,
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

  it('renders resume list on /resumes', () => {
    render(
      <MemoryRouter initialEntries={['/resumes']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resume-list-page')).toBeInTheDocument()
  })

  it('renders resume detail on /resumes/:groupId', () => {
    render(
      <MemoryRouter initialEntries={['/resumes/group-123']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resume-detail-page')).toBeInTheDocument()
  })

  it('renders resume edit on /resumes/:groupId/versions/:versionId/edit', () => {
    render(
      <MemoryRouter initialEntries={['/resumes/group-123/versions/v1/edit']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('resume-edit-page')).toBeInTheDocument()
  })

  it('renders job list on /jobs', () => {
    render(
      <MemoryRouter initialEntries={['/jobs']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('job-list-page')).toBeInTheDocument()
  })

  it('renders job detail on /jobs/:jobId', () => {
    render(
      <MemoryRouter initialEntries={['/jobs/job-456']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('job-detail-page')).toBeInTheDocument()
  })

  it('renders chat on /chat', () => {
    render(
      <MemoryRouter initialEntries={['/chat']}>
        <App />
      </MemoryRouter>
    )
    expect(screen.getByTestId('chat-page')).toBeInTheDocument()
  })

  it('renders tracking on /applications', () => {
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
    expect(toaster).toHaveAttribute('data-offset', '88px')
    expect(toaster).toHaveAttribute('data-mobile-offset', '72px')
  })
})
