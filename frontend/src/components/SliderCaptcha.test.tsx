import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import SliderCaptcha from './SliderCaptcha'

const mockGetCaptchaChallenge = vi.fn()
const mockVerifyCaptcha = vi.fn()

vi.mock('@/services/api', () => ({
  authService: {
    getCaptchaChallenge: () => mockGetCaptchaChallenge(),
    verifyCaptcha: (data: any) => mockVerifyCaptcha(data),
  },
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('lucide-react', () => ({
  Check: () => <span>✓</span>,
  X: () => <span>✕</span>,
  Loader2: () => <span>⏳</span>,
}))

// PointerEvent mock
class MockPointerEvent extends Event {
  clientX: number
  pointerId: number
  constructor(type: string, init: any = {}) {
    super(type, init)
    this.clientX = init.clientX ?? 0
    this.pointerId = init.pointerId ?? 1
  }
}
// @ts-ignore
window.PointerEvent = MockPointerEvent

describe('SliderCaptcha', () => {
  const onVerified = vi.fn()
  const onError = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    mockGetCaptchaChallenge.mockReset()
    mockVerifyCaptcha.mockReset()
  })

  it('loads challenge on mount', () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    expect(mockGetCaptchaChallenge).toHaveBeenCalled()
  })

  it('shows skeleton when challenge not loaded', () => {
    mockGetCaptchaChallenge.mockImplementation(() => new Promise(() => {}))

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    expect(screen.getByRole('presentation')).toBeInTheDocument()
  })

  it('renders track, handle, and target marker when challenge loaded', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })
  })

  it('calls onVerified when captcha succeeds', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })
    mockVerifyCaptcha.mockResolvedValue({ captchaToken: 'token-123' })

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    const handle = screen.getByText('→').closest('div')!

    // Simulate drag to target
    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientX: 120, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientX: 120, pointerId: 1 })

    await waitFor(() => {
      expect(mockVerifyCaptcha).toHaveBeenCalled()
      expect(onVerified).toHaveBeenCalledWith('token-123')
    })
  })

  it('calls onError and reloads challenge on failure', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })
    mockVerifyCaptcha.mockRejectedValue(new Error('Verification failed'))

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    const handle = screen.getByText('→').closest('div')!

    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientX: 50, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientX: 50, pointerId: 1 })

    await waitFor(() => {
      expect(onError).toHaveBeenCalled()
    })

    // Advance timer for reload
    vi.advanceTimersByTime(1000)

    vi.useRealTimers()
  })

  it('disables dragging while verifying', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })
    mockVerifyCaptcha.mockImplementation(() => new Promise(() => {}))

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    const handle = screen.getByText('→').closest('div')!

    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientX: 120, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientX: 120, pointerId: 1 })

    await waitFor(() => {
      expect(screen.getByText('⏳')).toBeInTheDocument()
    })

    // Second drag should be ignored while verifying
    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 2 })
    expect(mockGetCaptchaChallenge).toHaveBeenCalledTimes(1)
  })

  it('shows success state after verification', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })
    mockVerifyCaptcha.mockResolvedValue({ captchaToken: 'token-123' })

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    const handle = screen.getByText('→').closest('div')!

    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientX: 120, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientX: 120, pointerId: 1 })

    await waitFor(() => {
      expect(screen.getByText('✓')).toBeInTheDocument()
      expect(screen.getByText('auth.captcha.success')).toBeInTheDocument()
    })
  })

  it('shows error state after failed verification', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 120,
    })
    mockVerifyCaptcha.mockRejectedValue(new Error('Failed'))

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    const handle = screen.getByText('→').closest('div')!

    fireEvent.pointerDown(handle, { clientX: 0, pointerId: 1 })
    fireEvent.pointerMove(handle, { clientX: 50, pointerId: 1 })
    fireEvent.pointerUp(handle, { clientX: 50, pointerId: 1 })

    await waitFor(() => {
      expect(screen.getByText('✕')).toBeInTheDocument()
      expect(screen.getByText('auth.captcha.fail')).toBeInTheDocument()
    })
  })

  it('calls onError when challenge load fails', async () => {
    mockGetCaptchaChallenge.mockRejectedValue(new Error('Load failed'))

    render(<SliderCaptcha onVerified={onVerified} onError={onError} width={300} />)

    await waitFor(() => {
      expect(onError).toHaveBeenCalled()
    })
  })

  it('applies custom width', async () => {
    mockGetCaptchaChallenge.mockResolvedValue({
      captchaId: 'cap-1',
      targetX: 80,
    })

    const { container } = render(
      <SliderCaptcha onVerified={onVerified} onError={onError} width={200} className="custom-class" />
    )

    await waitFor(() => {
      expect(screen.getByText('auth.captcha.dragHint')).toBeInTheDocument()
    })

    expect(container.firstChild).toHaveClass('custom-class')
  })
})
