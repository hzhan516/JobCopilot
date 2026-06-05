import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import ErrorBoundary from './ErrorBoundary'

// Component that throws on demand
function Bomb({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) {
    throw new Error('Test explosion')
  }
  return <div>Safe content</div>
}

describe('ErrorBoundary', () => {
  it('renders children when no error', () => {
    render(
      <ErrorBoundary>
        <div>Normal content</div>
      </ErrorBoundary>
    )

    expect(screen.getByText('Normal content')).toBeInTheDocument()
  })

  it('catches errors and shows fallback UI', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    )

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('Try Again')).toBeInTheDocument()
    expect(screen.getByText('Back to Home')).toBeInTheDocument()

    consoleSpy.mockRestore()
  })

  it('shows custom fallback when provided', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary fallback={<div>Custom error screen</div>}>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    )

    expect(screen.getByText('Custom error screen')).toBeInTheDocument()

    consoleSpy.mockRestore()
  })

  it('resets error state when clicking Try Again', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const onReset = vi.fn()

    const { rerender } = render(
      <ErrorBoundary onReset={onReset}>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    )

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()

    // Click reset
    screen.getByText('Try Again').click()

    expect(onReset).toHaveBeenCalled()

    consoleSpy.mockRestore()
  })

  it('displays error message in the UI', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    render(
      <ErrorBoundary>
        <Bomb shouldThrow={true} />
      </ErrorBoundary>
    )

    expect(screen.getByText('Test explosion')).toBeInTheDocument()

    consoleSpy.mockRestore()
  })
})
