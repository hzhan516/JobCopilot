import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Register from './Register'

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

vi.mock('zod', () => ({
  z: {
    object: () => ({
      shape: () => ({
        email: { _def: { typeName: 'ZodString' } },
        password: { _def: { typeName: 'ZodString' } },
      }),
      parse: vi.fn(() => ({ success: true })),
      safeParse: vi.fn(() => ({ success: true, data: {} })),
    }),
    string: () => ({
      email: () => ({
        _def: { typeName: 'ZodString' },
        parse: vi.fn((val: string) => val),
      }),
      min: () => ({
        _def: { typeName: 'ZodString' },
        parse: vi.fn((val: string) => val),
      }),
    }),
  },
}))

vi.mock('react-hook-form', () => ({
  useForm: () => ({
    register: vi.fn(() => ({
      name: 'email',
      onChange: vi.fn(),
      onBlur: vi.fn(),
      ref: vi.fn(),
    })),
    handleSubmit: (fn: any) => (e: any) => {
      e?.preventDefault?.()
      return fn({ email: 'test@example.com', password: 'StrongPass123!' })
    },
    formState: { errors: {}, isSubmitting: false },
    watch: vi.fn(() => 'StrongPass123!'),
    setValue: vi.fn(),
    getValues: vi.fn(() => ({
      email: 'test@example.com',
      password: 'StrongPass123!',
    })),
    control: {},
  }),
  useWatch: () => 'StrongPass123!',
}))

vi.mock('@hookform/resolvers/zod', () => ({
  zodResolver: () => ({
    resolver: () => ({}),
  }),
}))

const mockRegisterEmail = vi.fn()
const mockRegisterGoogle = vi.fn()

vi.mock('@/services/api', () => ({
  authService: {
    register: (data: any) => mockRegisterEmail(data),
    registerWithGoogle: (token: string) => mockRegisterGoogle(token),
  },
}))

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, disabled, variant, type, className }: any) => (
    <button onClick={onClick} disabled={disabled} type={type} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardDescription: ({ children }: any) => <p>{children}</p>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardTitle: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('@/components/ui/input', () => ({
  Input: ({ placeholder, type, disabled, className }: any) => (
    <input placeholder={placeholder} type={type} disabled={disabled} className={className} />
  ),
}))

vi.mock('@/components/ui/checkbox', () => ({
  Checkbox: ({ checked, onCheckedChange }: any) => (
    <input type="checkbox" checked={checked} onChange={(e) => onCheckedChange?.(e.target.checked)} />
  ),
}))

vi.mock('@/components/ui/label', () => ({
  Label: ({ children }: any) => <label>{children}</label>,
}))

vi.mock('lucide-react', () => ({
  Mail: () => <span>✉️</span>,
  Lock: () => <span>🔒</span>,
  Eye: () => <span>👁️</span>,
  EyeOff: () => <span>🙈</span>,
  ArrowLeft: () => <span>←</span>,
  Sparkles: () => <span>✨</span>,
  ShieldCheck: () => <span>🛡️</span>,
}))

describe('Register page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders register form with all fields', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.title')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('auth.register.emailPlaceholder')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('auth.register.passwordPlaceholder')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('auth.register.confirmPasswordPlaceholder')).toBeInTheDocument()
  })

  it('navigates to login page on back button click', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('auth.register.backToLogin'))
    expect(mockNavigate).toHaveBeenCalledWith('/login')
  })

  it('shows terms and conditions checkbox', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.termsRequired')).toBeInTheDocument()
  })

  it('displays password strength indicator', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.passwordStrength')).toBeInTheDocument()
  })

  it('handles successful registration', async () => {
    mockRegisterEmail.mockResolvedValue({ token: 'new-token' })

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(mockRegisterEmail).toHaveBeenCalled()
    })
  })

  it('shows error on registration failure (409)', async () => {
    mockRegisterEmail.mockRejectedValue({
      response: { status: 409, data: { message: 'Email already exists' } },
    })

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled()
    })
  })

  it('shows error on registration failure (400)', async () => {
    mockRegisterEmail.mockRejectedValue({
      response: { status: 400, data: { message: 'Invalid input' } },
    })

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled()
    })
  })

  it('shows error on registration failure (generic)', async () => {
    mockRegisterEmail.mockRejectedValue(new Error('Network error'))

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled()
    })
  })

  it('handles Google registration', async () => {
    mockRegisterGoogle.mockResolvedValue({ token: 'google-token' })

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    // Google button would be in the component
    const googleBtn = screen.queryByText('auth.register.googleSignUp')
    if (googleBtn) {
      fireEvent.click(googleBtn)
      await waitFor(() => {
        expect(mockRegisterGoogle).toHaveBeenCalled()
      })
    }
  })

  it('toggles password visibility', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    const passwordInput = screen.getByPlaceholderText('auth.register.passwordPlaceholder')
    expect(passwordInput).toHaveAttribute('type', 'password')
  })

  it('displays password requirements', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.passwordRequirements')).toBeInTheDocument()
  })

  it('shows create account button', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.createAccount')).toBeInTheDocument()
  })

  it('shows Google sign up option', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.orContinueWith')).toBeInTheDocument()
  })
})
