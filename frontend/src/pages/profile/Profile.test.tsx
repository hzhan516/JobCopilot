import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Profile from './Profile'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
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
  user: { userId: 'u1', email: 'test@example.com', name: 'Test User' },
  logout: mockLogout,
  isAuthenticated: true,
}

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => mockAuth,
}))

const mockFetchProfile = vi.fn()
const mockUpdateProfile = vi.fn()
const mockUpdateAvatar = vi.fn()
let mockProfileStore = {
  profile: null as any,
  loading: false,
  fetchProfile: mockFetchProfile,
  updateProfile: mockUpdateProfile,
  updateAvatar: mockUpdateAvatar,
}

vi.mock('@/store/profile.store', () => ({
  useProfileStore: (selector?: any) => typeof selector === 'function' ? selector(mockProfileStore) : mockProfileStore,
}))

let mockTimeZone = 'America/Phoenix'
const mockUpdateTimeZone = vi.fn((tz: string) => { mockTimeZone = tz })
const mockResetTimeZone = vi.fn(() => { mockTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone })

vi.mock('@/hooks/useTimeZone', () => ({
  useTimeZone: () => ({
    timeZone: mockTimeZone,
    updateTimeZone: mockUpdateTimeZone,
    resetTimeZone: mockResetTimeZone,
  }),
}))

vi.mock('@/utils/i18n', () => ({
  getUserTimeZone: () => 'America/Phoenix',
  formatDate: (date: string) => date,
  formatDateTime: (date: string) => date,
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, disabled, variant, type, className }: any) => (
    <button onClick={onClick} disabled={disabled} type={type} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/ui/input', () => ({
  Input: ({ value, onChange, placeholder, type }: any) => (
    <input value={value || ''} onChange={onChange} placeholder={placeholder} type={type} />
  ),
}))

vi.mock('@/components/ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardTitle: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('@/components/ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => <span className={className}>{children}</span>,
}))

vi.mock('@/components/ui/select', () => ({
  Select: ({ children, value, onValueChange }: any) => (
    <div data-testid="timezone-select" data-value={value}>
      {children}
    </div>
  ),
  SelectTrigger: ({ children }: any) => <button>{children}</button>,
  SelectContent: ({ children }: any) => <div>{children}</div>,
  SelectItem: ({ children, value }: any) => (
    <button onClick={() => onValueChange?.(value)}>{children}</button>
  ),
  SelectValue: () => <span>Select</span>,
}))

vi.mock('@/components/ui/form', () => ({
  Form: ({ children }: any) => <form>{children}</form>,
  FormControl: ({ children }: any) => <div>{children}</div>,
  FormField: ({ render, name }: any) => render({ field: { value: '', onChange: vi.fn(), name } }),
  FormItem: ({ children }: any) => <div>{children}</div>,
  FormLabel: ({ children }: any) => <label>{children}</label>,
  FormMessage: () => null,
}))

vi.mock('lucide-react', () => ({
  User: () => <span>👤</span>,
  Mail: () => <span>✉️</span>,
  Shield: () => <span>🛡️</span>,
  LogOut: () => <span>🚪</span>,
  ArrowLeft: () => <span>←</span>,
  Lock: () => <span>🔒</span>,
  Link: () => <span>🔗</span>,
  Sparkles: () => <span>✨</span>,
  Phone: () => <span>📞</span>,
  Briefcase: () => <span>💼</span>,
  MapPin: () => <span>📍</span>,
  Save: () => <span>💾</span>,
  Loader2: () => <span>⏳</span>,
  Globe: () => <span>🌍</span>,
}))

describe('Profile page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockAuth = {
      user: { userId: 'u1', email: 'test@example.com', name: 'Test User' },
      logout: mockLogout,
      isAuthenticated: true,
    }
    mockProfileStore = {
      profile: null,
      loading: false,
      fetchProfile: mockFetchProfile,
      updateProfile: mockUpdateProfile,
      updateAvatar: mockUpdateAvatar,
    }
    mockTimeZone = 'America/Phoenix'
  })

  it('renders profile page with title and subtitle', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('profile.title')).toBeInTheDocument()
    expect(screen.getByText('profile.subtitle')).toBeInTheDocument()
  })

  it('fetches profile on mount', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(mockFetchProfile).toHaveBeenCalled()
  })

  it('renders user email from auth', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('test@example.com')).toBeInTheDocument()
  })

  it('renders user id', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('u1')).toBeInTheDocument()
  })

  it('syncs form with loaded profile', () => {
    mockProfileStore = {
      ...mockProfileStore,
      profile: {
        fullName: 'John Doe',
        phone: '1234567890',
        targetPosition: 'Senior Dev',
        preferredLocation: 'Remote',
        avatarUrl: 'https://example.com/avatar.png',
      },
    }

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )
  })

  it('submits profile update', async () => {
    mockProfileStore = {
      ...mockProfileStore,
      profile: {
        fullName: 'John',
        phone: '',
        targetPosition: '',
        preferredLocation: '',
        avatarUrl: '',
      },
    }
    mockUpdateProfile.mockResolvedValue({})
    mockUpdateAvatar.mockResolvedValue({})

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    // The form is rendered but react-hook-form + zod mock makes it hard to test direct interaction
    // Test through the onSubmit handler by finding and clicking the save button
    const saveButton = screen.getByText('common.save')
    fireEvent.click(saveButton)

    await waitFor(() => {
      expect(mockUpdateProfile).toHaveBeenCalled()
      expect(mockUpdateAvatar).toHaveBeenCalled()
    })
  })

  it('shows loading state on save button', () => {
    mockProfileStore = {
      ...mockProfileStore,
      loading: true,
    }

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('⏳')).toBeInTheDocument()
  })

  it('shows error toast when update fails', async () => {
    mockProfileStore = {
      ...mockProfileStore,
      profile: {
        fullName: 'John',
        phone: '',
        targetPosition: '',
        preferredLocation: '',
        avatarUrl: '',
      },
    }
    mockUpdateProfile.mockRejectedValue(new Error('Update failed'))

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    const saveButton = screen.getByText('common.save')
    fireEvent.click(saveButton)

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('profile.saveError')
    })
  })

  it('navigates back on back button click', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('common.back'))
    expect(mockNavigate).toHaveBeenCalledWith('/')
  })

  it('handles logout and navigates to login', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('layout.userMenu.logout'))

    expect(mockLogout).toHaveBeenCalled()
    expect(mockNavigate).toHaveBeenCalledWith('/login')
  })

  it('renders timezone selector', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByTestId('timezone-select')).toBeInTheDocument()
  })

  it('renders security section', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('profile.security')).toBeInTheDocument()
    expect(screen.getByText('profile.changePassword')).toBeInTheDocument()
  })

  it('renders AI features section', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('profile.aiFeatures')).toBeInTheDocument()
    expect(screen.getByText('profile.resumeOptimization')).toBeInTheDocument()
  })

  it('navigates to resumes from AI features', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('profile.goToResumes'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes')
  })

  it('renders preferences section with timezone', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByText('profile.preferences')).toBeInTheDocument()
    expect(screen.getByText('profile.timeZone')).toBeInTheDocument()
  })

  it('renders avatar with image when avatarUrl is set', () => {
    mockProfileStore = {
      ...mockProfileStore,
      profile: {
        fullName: 'John',
        phone: '',
        targetPosition: '',
        preferredLocation: '',
        avatarUrl: 'https://example.com/avatar.png',
      },
    }

    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getByAltText('profile.avatarAlt')).toBeInTheDocument()
  })

  it('renders avatar fallback when no avatarUrl', () => {
    render(
      <MemoryRouter>
        <Profile />
      </MemoryRouter>
    )

    expect(screen.getAllByText('👤').length).toBeGreaterThan(0)
  })
})
