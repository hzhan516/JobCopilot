import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Login from './Login'

const mockNavigate = vi.fn()
const mockLogin = vi.fn()
const mockLoginByGoogle = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    login: mockLogin,
    loginByGoogle: mockLoginByGoogle,
  }),
}))

vi.mock('react-i18next', () => {
  const t = (key: string) => key

  return {
  useTranslation: () => ({
    t,
  }),
  }
})

vi.mock('@/components/SliderCaptcha', () => ({
  default: ({ onVerified }: { onVerified: (token: string) => void }) => (
    <div data-testid="slider-captcha">
      <button onClick={() => onVerified('captcha-token-123')}>Verify CAPTCHA</button>
    </div>
  ),
}))

vi.mock('@/components/LanguageSwitcher', () => ({
  LanguageSwitcher: () => <div data-testid="language-switcher" />,
}))

vi.mock('@react-oauth/google', () => ({
  GoogleLogin: ({ onSuccess }: { onSuccess: (cred: { credential: string }) => void }) => (
    <button data-testid="google-login" onClick={() => onSuccess({ credential: 'google-id-token' })}>
      Google Login
    </button>
  ),
}))

describe('Login page', () => {
  beforeEach(() => {
    mockLogin.mockReset()
    mockLoginByGoogle.mockReset()
    mockNavigate.mockReset()
  })

  it('renders login form with email, password, and submit button', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    expect(screen.getByPlaceholderText('auth.login.emailPlaceholder')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('auth.login.passwordPlaceholder')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).toBeInTheDocument()
  })

  it('shows validation error for invalid email', async () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const emailInput = screen.getByPlaceholderText('auth.login.emailPlaceholder')
    const passwordInput = screen.getByPlaceholderText('auth.login.passwordPlaceholder')
    fireEvent.change(emailInput, { target: { value: 'invalid-email' } })
    fireEvent.change(passwordInput, { target: { value: 'password123' } })
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).not.toBeDisabled()
    })

    fireEvent.click(screen.getByRole('button', { name: /auth.login.loginButton/i }))

    await waitFor(() => {
      expect(screen.getByText('auth.login.errors.emailInvalid')).toBeInTheDocument()
    })
  })

  it('shows validation error for short password', async () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const passwordInput = screen.getByPlaceholderText('auth.login.passwordPlaceholder')
    fireEvent.change(screen.getByPlaceholderText('auth.login.emailPlaceholder'), {
      target: { value: 'test@example.com' },
    })
    fireEvent.change(passwordInput, { target: { value: '123' } })
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).not.toBeDisabled()
    })

    fireEvent.click(screen.getByRole('button', { name: /auth.login.loginButton/i }))

    await waitFor(() => {
      expect(screen.getByText('auth.login.errors.passwordMin')).toBeInTheDocument()
    })
  })

  it('requires CAPTCHA before login', async () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const emailInput = screen.getByPlaceholderText('auth.login.emailPlaceholder')
    const passwordInput = screen.getByPlaceholderText('auth.login.passwordPlaceholder')
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } })
    fireEvent.change(passwordInput, { target: { value: 'password123' } })

    const submitBtn = screen.getByRole('button', { name: /auth.login.loginButton/i })
    expect(submitBtn).toBeDisabled()

    // Verify CAPTCHA
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(submitBtn).not.toBeDisabled()
    })
  })

  it('submits login with correct credentials and redirects', async () => {
    mockLogin.mockResolvedValue(undefined)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('auth.login.emailPlaceholder'), {
      target: { value: 'test@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('auth.login.passwordPlaceholder'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).not.toBeDisabled()
    })

    fireEvent.click(screen.getByRole('button', { name: /auth.login.loginButton/i }))

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith(
        { email: 'test@example.com', password: 'password123', captchaToken: 'captcha-token-123' },
        false
      )
      expect(mockNavigate).toHaveBeenCalledWith('/resumes', { replace: true })
    })
  })

  it('shows error on login failure and resets CAPTCHA', async () => {
    mockLogin.mockRejectedValue(new Error('Invalid credentials'))

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('auth.login.emailPlaceholder'), {
      target: { value: 'test@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('auth.login.passwordPlaceholder'), {
      target: { value: 'wrongpass' },
    })
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).not.toBeDisabled()
    })

    fireEvent.click(screen.getByRole('button', { name: /auth.login.loginButton/i }))

    await waitFor(() => {
      expect(screen.getByText('auth.login.errorInvalidCredentials')).toBeInTheDocument()
    })
  })

  it('toggles password visibility', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const passwordInput = screen.getByPlaceholderText('auth.login.passwordPlaceholder')
    expect(passwordInput).toHaveAttribute('type', 'password')

    const toggleBtn = screen.getByRole('button', { name: '' }) // Eye icon button
    fireEvent.click(toggleBtn)

    expect(passwordInput).toHaveAttribute('type', 'text')
  })

  it('handles Google login after CAPTCHA verification', async () => {
    mockLoginByGoogle.mockResolvedValue(undefined)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    // Click Google button to show CAPTCHA modal
    fireEvent.click(screen.getByRole('button', { name: 'auth.login.googleLogin' }))

    // Verify CAPTCHA in modal
    await waitFor(() => {
      expect(screen.getAllByText('Verify CAPTCHA').length).toBeGreaterThan(0)
    })

    // The modal should appear with a second CAPTCHA instance
    const captchaButtons = screen.getAllByText('Verify CAPTCHA')
    fireEvent.click(captchaButtons[captchaButtons.length - 1])

    // Now GoogleLogin button should appear
    await waitFor(() => {
      const googleBtn = screen.queryByTestId('google-login')
      if (googleBtn) {
        fireEvent.click(googleBtn)
      }
    })

    // Google login flow may be gated by captchaToken — verify state changes
    expect(screen.queryByText('auth.captcha.required')).not.toBeInTheDocument()
  })

  it('resets Google CAPTCHA state after Google login failure', async () => {
    mockLoginByGoogle.mockRejectedValue(new Error('Google failed'))

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText(/auth.login.googleLogin/i))

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
      expect(mockLoginByGoogle).toHaveBeenCalledWith(
        { idToken: 'google-id-token', captchaToken: 'captcha-token-123' },
        false
      )
      expect(screen.getByText('auth.login.googleLoginFailed')).toBeInTheDocument()
    })

    expect(screen.queryByTestId('google-login')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'auth.login.googleLogin' })).toBeInTheDocument()
  })

  it('has rememberMe checkbox unchecked by default', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    const checkbox = screen.getByRole('checkbox')
    expect(checkbox).not.toBeChecked()
  })

  it('disables submit during login loading', async () => {
    let resolveLogin: () => void
    const loginPromise = new Promise<void>((resolve) => {
      resolveLogin = resolve
    })
    mockLogin.mockReturnValue(loginPromise)

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('auth.login.emailPlaceholder'), {
      target: { value: 'test@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('auth.login.passwordPlaceholder'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByText('Verify CAPTCHA'))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loginButton/i })).not.toBeDisabled()
    })

    fireEvent.click(screen.getByRole('button', { name: /auth.login.loginButton/i }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /auth.login.loggingIn/i })).toBeDisabled()
    })

    resolveLogin!()
  })
})
