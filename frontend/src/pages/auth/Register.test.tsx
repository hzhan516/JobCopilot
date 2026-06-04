import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import Register from './Register'

const mockNavigate = vi.hoisted(() => vi.fn())

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => ({
  initReactI18next: {
    type: '3rdParty',
    init: vi.fn(),
  },
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('zod', () => {
  const stringSchema = {
    min: () => stringSchema,
    max: () => stringSchema,
    email: () => stringSchema,
    regex: () => stringSchema,
    optional: () => stringSchema,
  }
  const booleanSchema = {
    refine: () => booleanSchema,
  }
  const objectSchema = {
    refine: () => objectSchema,
  }

  return {
    z: {
      object: () => objectSchema,
      string: () => stringSchema,
      boolean: () => booleanSchema,
    },
  }
})

vi.mock('react-hook-form', () => ({
  FormProvider: ({ children }: any) => <>{children}</>,
  Controller: ({ name, render }: any) =>
    render({
      field: {
        name,
        value:
          name === 'password' || name === 'confirmPassword'
            ? 'StrongPass123!'
            : name === 'agreeTerms'
              ? true
              : '',
        onChange: vi.fn(),
        onBlur: vi.fn(),
        ref: vi.fn(),
      },
    }),
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
    trigger: vi.fn().mockResolvedValue(true),
    getFieldState: vi.fn(() => ({})),
    control: {},
  }),
  useFormContext: () => ({
    getFieldState: vi.fn(() => ({})),
  }),
  useFormState: () => ({}),
  useWatch: () => 'StrongPass123!',
}))

vi.mock('@hookform/resolvers/zod', () => ({
  zodResolver: () => ({
    resolver: () => ({}),
  }),
}))

vi.mock('@react-oauth/google', () => ({
  GoogleLogin: ({ onSuccess }: { onSuccess: (cred: { credential: string }) => void }) => (
    <button data-testid="google-login" onClick={() => onSuccess({ credential: 'google-id-token' })}>
      Google Login
    </button>
  ),
}))

const mockRegisterEmail = vi.hoisted(() => vi.fn())
const mockRegisterGoogle = vi.hoisted(() => vi.fn())

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    register: (data: any, rememberMe: boolean) => mockRegisterEmail(data, rememberMe),
    loginByGoogle: (data: any, rememberMe: boolean) => mockRegisterGoogle(data, rememberMe),
  }),
}))

vi.mock('@/services/api', () => ({
  authService: {
    isEmailVerificationEnabled: vi.fn().mockResolvedValue(false),
    sendVerificationCode: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('@/components/SliderCaptcha', () => ({
  default: ({ onVerified }: { onVerified: (token: string) => void }) => (
    <button type="button" onClick={() => onVerified('captcha-token')}>Verify CAPTCHA</button>
  ),
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
  CardFooter: ({ children, className }: any) => <div className={className}>{children}</div>,
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
  FileText: () => <span>📄</span>,
  Loader2: () => <span>⏳</span>,
  Globe: () => <span>🌐</span>,
  Check: () => <span>✓</span>,
  XIcon: () => <span>×</span>,
}))

function LocationStateDisplay() {
  const location = useLocation()
  const state = location.state as { from?: { pathname?: string } } | null

  return <div data-testid="from-path">{state?.from?.pathname ?? 'no-state'}</div>
}

describe('Register page', () => {
  const verifyCaptcha = () => {
    fireEvent.click(screen.getByText('Verify CAPTCHA'))
  }

  beforeEach(() => {
    mockNavigate.mockReset()
    mockRegisterEmail.mockReset()
    mockRegisterGoogle.mockReset()
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

  it('links to login page', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.loginNow').closest('a')).toHaveAttribute('href', '/login')
  })

  it('preserves saved protected route state when switching to login', () => {
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/register',
            state: {
              from: {
                pathname: '/applications',
                search: '?edit=tracking-1',
                hash: '#timeline',
              },
            },
          },
        ]}
      >
        <Routes>
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<LocationStateDisplay />} />
        </Routes>
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('auth.register.loginNow'))

    expect(screen.getByTestId('from-path')).toHaveTextContent('/applications')
  })

  it('shows terms and conditions checkbox', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(document.body).toHaveTextContent('auth.register.agreeTerms')
    expect(screen.getByText('auth.register.termsOfService')).toBeInTheDocument()
    expect(screen.getByText('auth.register.privacyPolicy')).toBeInTheDocument()
  })

  it('displays password strength indicator', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.passwordStrength.strong')).toBeInTheDocument()
    expect(screen.getByText('auth.register.passwordStrength.hint')).toBeInTheDocument()
  })

  it('handles successful registration', async () => {
    mockRegisterEmail.mockResolvedValue({ token: 'new-token' })

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    verifyCaptcha()

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(mockRegisterEmail).toHaveBeenCalled()
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
    })
  })

  it('redirects back to saved protected route after successful registration', async () => {
    mockRegisterEmail.mockResolvedValue({ token: 'new-token' })

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/register',
            state: {
              from: {
                pathname: '/applications',
                search: '?edit=tracking-1',
                hash: '#timeline',
              },
            },
          },
        ]}
      >
        <Register />
      </MemoryRouter>
    )

    verifyCaptcha()

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    await waitFor(() => {
      expect(mockRegisterEmail).toHaveBeenCalled()
      expect(mockNavigate).toHaveBeenCalledWith('/applications?edit=tracking-1#timeline', { replace: true })
    })
  })

  it('shows error on registration failure (409)', async () => {
    mockRegisterEmail.mockRejectedValue({
      response: { status: 409, data: { message: 'Email already exists' } },
    })

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    verifyCaptcha()

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    expect(await screen.findByRole('alert')).toHaveTextContent('auth.register.errorEmailExists')
  })

  it('shows error on registration failure (400)', async () => {
    mockRegisterEmail.mockRejectedValue({
      response: { status: 400, data: { message: 'Invalid input' } },
    })

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    verifyCaptcha()

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid input')
  })

  it('shows error on registration failure (generic)', async () => {
    mockRegisterEmail.mockRejectedValue(new Error('Network error'))

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    verifyCaptcha()

    const form = screen.getByText('auth.register.title').closest('form') || document.querySelector('form')
    if (form) {
      fireEvent.submit(form)
    }

    expect(await screen.findByRole('alert')).toHaveTextContent('Network error')
  })

  it('shows Google registration entry point', async () => {
    mockRegisterGoogle.mockResolvedValue({ token: 'google-token' })

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.login.googleLogin')).toBeInTheDocument()
  })

  it('resets Google CAPTCHA state after Google registration failure', async () => {
    mockRegisterGoogle.mockRejectedValue(new Error('Google failed'))

    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByRole('button', { name: 'auth.login.googleLogin' }))

    await waitFor(() => {
      expect(screen.getAllByText('Verify CAPTCHA').length).toBeGreaterThan(0)
    })

    const captchaButtons = screen.getAllByText('Verify CAPTCHA')
    fireEvent.click(captchaButtons[captchaButtons.length - 1])

    await waitFor(() => {
      expect(screen.getByTestId('google-login')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('google-login'))

    await waitFor(() => {
      expect(mockRegisterGoogle).toHaveBeenCalledWith(
        { idToken: 'google-id-token', captchaToken: 'captcha-token' },
        false
      )
      expect(screen.getByRole('alert')).toHaveTextContent('auth.login.googleLoginFailed')
    })

    expect(screen.queryByTestId('google-login')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'auth.login.googleLogin' })).toBeInTheDocument()
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

    expect(screen.getByText('auth.register.passwordStrength.hint')).toBeInTheDocument()
  })

  it('shows create account button', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.register.registerButton')).toBeInTheDocument()
  })

  it('shows Google sign up option', () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    )

    expect(screen.getByText('auth.login.or')).toBeInTheDocument()
    expect(screen.getByText('auth.login.googleLogin')).toBeInTheDocument()
  })
})
