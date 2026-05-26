import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AIOptimizeCompare } from './AIOptimizeCompare'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('../ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('../ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => (
    <span data-variant={variant} className={className}>{children}</span>
  ),
}))

vi.mock('../ui/button', () => ({
  Button: ({ children, onClick, variant, size, className }: any) => (
    <button onClick={onClick} data-variant={variant} className={className}>{children}</button>
  ),
}))

vi.mock('@uiw/react-md-editor', () => ({
  default: Object.assign(({ source }: any) => <div data-testid="md-preview">{source}</div>, {
    Markdown: ({ source }: any) => <div data-testid="md-preview">{source}</div>,
  }),
  Markdown: ({ source }: any) => <div data-testid="md-preview">{source}</div>,
}))

vi.mock('lucide-react', () => ({
  ArrowRight: () => <span>→</span>,
  FileText: () => <span>📄</span>,
  Sparkles: () => <span>✨</span>,
}))

const createVersion = (overrides = {}) => ({
  versionId: 'v1',
  versionType: 'ORIGINAL' as const,
  status: 'ACTIVE' as const,
  parseStatus: 'COMPLETED' as const,
  content: 'Original resume content',
  createdAt: '2024-01-15T00:00:00Z',
  ...overrides,
})

describe('AIOptimizeCompare', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders empty state when no versions', () => {
    render(
      <AIOptimizeCompare originalVersion={null} aiOptimizedVersion={null} />
    )

    expect(screen.getByText('resume.compare.noVersions')).toBeInTheDocument()
    expect(screen.getByText('resume.compare.uploadFirst')).toBeInTheDocument()
  })

  it('renders split view by default', () => {
    const original = createVersion()
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      content: 'AI optimized content',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    expect(screen.getByText('resume.compare.title')).toBeInTheDocument()
    expect(screen.getByText('Original resume content')).toBeInTheDocument()
    expect(screen.getByText('AI optimized content')).toBeInTheDocument()
  })

  it('switches to original only view', () => {
    const original = createVersion()
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      content: 'AI optimized',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    fireEvent.click(screen.getByText('resume.compare.originalOnly'))
    expect(screen.getByText('Original resume content')).toBeInTheDocument()
    expect(screen.queryByText('AI optimized')).not.toBeInTheDocument()
  })

  it('switches to AI only view', () => {
    const original = createVersion()
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      content: 'AI optimized',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    fireEvent.click(screen.getByText('resume.compare.aiOnly'))
    expect(screen.getByText('AI optimized')).toBeInTheDocument()
    expect(screen.queryByText('Original resume content')).not.toBeInTheDocument()
  })

  it('shows split view button as active in default state', () => {
    const original = createVersion()
    const aiOptimized = createVersion({ versionId: 'v2', versionType: 'AI_OPTIMIZED' })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const splitBtn = screen.getByText('resume.compare.splitView')
    expect(splitBtn).toHaveAttribute('data-variant', 'default')
  })

  it('shows version not available for missing original', () => {
    const aiOptimized = createVersion({ versionId: 'v2', versionType: 'AI_OPTIMIZED' })

    render(
      <AIOptimizeCompare originalVersion={null} aiOptimizedVersion={aiOptimized} />
    )

    expect(screen.getByText(/resume.compare.versionNotAvailable/)).toBeInTheDocument()
  })

  it('shows version not available for missing AI optimized', () => {
    const original = createVersion()

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={null} />
    )

    expect(screen.getByText(/resume.compare.versionNotAvailable/)).toBeInTheDocument()
  })

  it('shows correct badges for completed status', () => {
    const original = createVersion({ parseStatus: 'COMPLETED' })
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      parseStatus: 'COMPLETED',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const badges = screen.getAllByText('resume.parseStatus.completed')
    expect(badges.length).toBe(2)
  })

  it('shows correct badges for parsing status', () => {
    const original = createVersion({ parseStatus: 'PARSING' })
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      parseStatus: 'PARSING',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const badges = screen.getAllByText('resume.parseStatus.parsing')
    expect(badges.length).toBe(2)
  })

  it('shows correct badges for failed status', () => {
    const original = createVersion({ parseStatus: 'FAILED' })
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      parseStatus: 'FAILED',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const badges = screen.getAllByText('resume.parseStatus.failed')
    expect(badges.length).toBe(2)
  })

  it('shows not applicable badge', () => {
    const original = createVersion({ parseStatus: 'NOT_APPLICABLE' })
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      parseStatus: 'NOT_APPLICABLE',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const badges = screen.getAllByText('resume.parseStatus.notApplicable')
    expect(badges.length).toBe(2)
  })

  it('shows content not available when content is empty', () => {
    const original = createVersion({ content: '' })
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      content: '',
    })

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={aiOptimized} />
    )

    const notAvailableTexts = screen.getAllByText('resume.versionDetail.contentNotAvailable')
    expect(notAvailableTexts.length).toBe(2)
  })

  it('renders with only original version', () => {
    const original = createVersion()

    render(
      <AIOptimizeCompare originalVersion={original} aiOptimizedVersion={null} />
    )

    fireEvent.click(screen.getByText('resume.compare.originalOnly'))
    expect(screen.getByText('Original resume content')).toBeInTheDocument()
  })

  it('renders with only AI version', () => {
    const aiOptimized = createVersion({
      versionId: 'v2',
      versionType: 'AI_OPTIMIZED',
      content: 'AI content',
    })

    render(
      <AIOptimizeCompare originalVersion={null} aiOptimizedVersion={aiOptimized} />
    )

    fireEvent.click(screen.getByText('resume.compare.aiOnly'))
    expect(screen.getByText('AI content')).toBeInTheDocument()
  })
})
