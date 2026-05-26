import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ParseStatusBadge } from './ParseStatusBadge'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('../ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => (
    <span data-variant={variant} className={className}>{children}</span>
  ),
}))

vi.mock('lucide-react', () => ({
  Loader2: () => <span data-testid="loader-icon">⏳</span>,
  CheckCircle: () => <span data-testid="check-icon">✓</span>,
  XCircle: () => <span data-testid="x-icon">✕</span>,
  Clock: () => <span data-testid="clock-icon">🕐</span>,
}))

describe('ParseStatusBadge', () => {
  it('renders PENDING status with clock icon', () => {
    render(<ParseStatusBadge status="PENDING" />)

    expect(screen.getByText('resume.parseStatus.pending')).toBeInTheDocument()
    expect(screen.getByTestId('clock-icon')).toBeInTheDocument()
  })

  it('renders PARSING status with loader icon', () => {
    render(<ParseStatusBadge status="PARSING" />)

    expect(screen.getByText('resume.parseStatus.parsing')).toBeInTheDocument()
    expect(screen.getByTestId('loader-icon')).toBeInTheDocument()
  })

  it('renders COMPLETED status with check icon', () => {
    render(<ParseStatusBadge status="COMPLETED" />)

    expect(screen.getByText('resume.parseStatus.completed')).toBeInTheDocument()
    expect(screen.getByTestId('check-icon')).toBeInTheDocument()
  })

  it('renders FAILED status with x icon', () => {
    render(<ParseStatusBadge status="FAILED" />)

    expect(screen.getByText('resume.parseStatus.failed')).toBeInTheDocument()
    expect(screen.getByTestId('x-icon')).toBeInTheDocument()
  })

  it('returns null for NOT_APPLICABLE status', () => {
    const { container } = render(<ParseStatusBadge status="NOT_APPLICABLE" />)

    expect(container.firstChild).toBeNull()
  })

  it('returns null for unknown status', () => {
    const { container } = render(<ParseStatusBadge status={"UNKNOWN" as any} />)

    expect(container.firstChild).toBeNull()
  })

  it('applies correct variant for each status', () => {
    const { rerender } = render(<ParseStatusBadge status="PENDING" />)
    expect(screen.getByText('resume.parseStatus.pending').parentElement).toHaveAttribute('data-variant', 'secondary')

    rerender(<ParseStatusBadge status="PARSING" />)
    expect(screen.getByText('resume.parseStatus.parsing').parentElement).toHaveAttribute('data-variant', 'secondary')

    rerender(<ParseStatusBadge status="COMPLETED" />)
    expect(screen.getByText('resume.parseStatus.completed').parentElement).toHaveAttribute('data-variant', 'secondary')

    rerender(<ParseStatusBadge status="FAILED" />)
    expect(screen.getByText('resume.parseStatus.failed').parentElement).toHaveAttribute('data-variant', 'destructive')
  })
})
