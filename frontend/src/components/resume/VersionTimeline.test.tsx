import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { VersionTimeline } from './VersionTimeline'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/utils/i18n', () => ({
  formatDate: () => 'Jan 15, 2024',
  formatTime: () => '10:30 AM',
}))

vi.mock('../ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => (
    <span data-variant={variant} className={className}>{children}</span>
  ),
}))

vi.mock('./ParseStatusBadge', () => ({
  ParseStatusBadge: ({ status }: any) => <span data-testid={`parse-${status}`}>{status}</span>,
}))

vi.mock('lucide-react', () => ({
  FileText: () => <span>📄</span>,
  FileCode2: () => <span>📜</span>,
  Sparkles: () => <span>✨</span>,
  Play: () => <span>▶</span>,
}))

const createVersions = () => [
  { versionId: 'v3', versionType: 'AI_OPTIMIZED' as const, createdAt: '2024-01-20T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
  { versionId: 'v2', versionType: 'CONVERTED' as const, createdAt: '2024-01-18T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
  { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
]

describe('VersionTimeline', () => {
  const mockOnSelect = vi.fn()
  const mockOnActivate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders all versions sorted by createdAt desc', () => {
    const versions = createVersions()

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    // Should show all 3 versions
    expect(screen.getByText('resume.timeline.aiOptimized - v3')).toBeInTheDocument()
    expect(screen.getByText('resume.timeline.converted - v2')).toBeInTheDocument()
    expect(screen.getByText('resume.timeline.original - v1')).toBeInTheDocument()
  })

  it('calls onSelectVersion when version clicked', () => {
    const versions = createVersions()

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    fireEvent.click(screen.getByText('resume.timeline.converted - v2'))
    expect(mockOnSelect).toHaveBeenCalledWith('v2')
  })

  it('shows active badge for ACTIVE status', () => {
    const versions = [
      { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByText('resume.timeline.active')).toBeInTheDocument()
    expect(screen.getByText('resume.timeline.active').parentElement).toHaveAttribute('data-variant', 'outline')
  })

  it('shows archived badge for ARCHIVED status', () => {
    const versions = [
      { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByText('resume.timeline.archived')).toBeInTheDocument()
    expect(screen.getByText('resume.timeline.archived').parentElement).toHaveAttribute('data-variant', 'secondary')
  })

  it('shows activate button for archived non-original versions', () => {
    const versions = [
      { versionId: 'v2', versionType: 'CONVERTED' as const, createdAt: '2024-01-18T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v2"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    expect(screen.getByText('resume.timeline.setActive')).toBeInTheDocument()
  })

  it('does not show activate button for original versions', () => {
    const versions = [
      { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    expect(screen.queryByText('resume.timeline.setActive')).not.toBeInTheDocument()
  })

  it('does not show activate button for active versions', () => {
    const versions = [
      { versionId: 'v2', versionType: 'CONVERTED' as const, createdAt: '2024-01-18T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v2"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    expect(screen.queryByText('resume.timeline.setActive')).not.toBeInTheDocument()
  })

  it('calls onActivateVersion when activate button clicked', () => {
    const versions = [
      { versionId: 'v2', versionType: 'CONVERTED' as const, createdAt: '2024-01-18T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v2"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    fireEvent.click(screen.getByText('resume.timeline.setActive'))
    expect(mockOnActivate).toHaveBeenCalledWith('v2')
  })

  it('stops propagation when activate button clicked', () => {
    const versions = [
      { versionId: 'v2', versionType: 'CONVERTED' as const, createdAt: '2024-01-18T00:00:00Z', status: 'ARCHIVED' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v2"
        onSelectVersion={mockOnSelect}
        onActivateVersion={mockOnActivate}
      />
    )

    const activateBtn = screen.getByText('resume.timeline.setActive')
    fireEvent.click(activateBtn)

    // onSelect should NOT be called because stopPropagation
    expect(mockOnSelect).not.toHaveBeenCalled()
  })

  it('shows correct icon for ORIGINAL version type', () => {
    const versions = [
      { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByText('📄')).toBeInTheDocument()
  })

  it('shows correct icon for CONVERTED version type', () => {
    const versions = [
      { versionId: 'v1', versionType: 'CONVERTED' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByText('📜')).toBeInTheDocument()
  })

  it('shows correct icon for AI_OPTIMIZED version type', () => {
    const versions = [
      { versionId: 'v1', versionType: 'AI_OPTIMIZED' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'COMPLETED' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByText('✨')).toBeInTheDocument()
  })

  it('shows parse status badge for each version', () => {
    const versions = [
      { versionId: 'v1', versionType: 'ORIGINAL' as const, createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE' as const, parseStatus: 'PARSING' as const },
    ]

    render(
      <VersionTimeline
        versions={versions}
        selectedVersionId="v1"
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.getByTestId('parse-PARSING')).toBeInTheDocument()
  })

  it('renders empty when versions array is empty', () => {
    render(
      <VersionTimeline
        versions={[]}
        selectedVersionId=""
        onSelectVersion={mockOnSelect}
      />
    )

    expect(screen.queryByText('resume.timeline.original')).not.toBeInTheDocument()
  })
})
