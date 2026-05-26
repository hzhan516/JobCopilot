import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { VersionDetail } from './VersionDetail'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('../ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardTitle: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('../ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => (
    <span data-variant={variant} className={className}>{children}</span>
  ),
}))

vi.mock('../ui/button', () => ({
  Button: ({ children, onClick, variant, size, className, disabled }: any) => (
    <button onClick={onClick} className={className} disabled={disabled}>{children}</button>
  ),
}))

vi.mock('./DownloadButton', () => ({
  DownloadButton: ({ versionId, filename }: any) => (
    <span data-testid="download-btn" data-version={versionId} data-filename={filename}>Download</span>
  ),
}))

vi.mock('lucide-react', () => ({
  Edit3: () => <span>✏️</span>,
  FileText: () => <span>📄</span>,
  Briefcase: () => <span>💼</span>,
  List: () => <span>📋</span>,
  Copy: () => <span>📋</span>,
  Play: () => <span>▶️</span>,
}))

const createVersion = (overrides = {}) => ({
  versionId: 'v1',
  versionType: 'ORIGINAL' as const,
  status: 'ACTIVE' as const,
  parseStatus: 'COMPLETED' as const,
  content: 'Resume content here',
  createdAt: '2024-01-15T00:00:00Z',
  ...overrides,
})

describe('VersionDetail', () => {
  const mockOnEdit = vi.fn()
  const mockOnCreateCopy = vi.fn()
  const mockOnActivate = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders title and type badge', () => {
    const version = createVersion({ versionType: 'ORIGINAL' })

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onCreateCopy={mockOnCreateCopy}
      />
    )

    expect(screen.getByText('resume.versionDetail.title')).toBeInTheDocument()
  })

  it('shows parsed badge for COMPLETED status', () => {
    const version = createVersion({ parseStatus: 'COMPLETED' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.versionDetail.parsed')).toBeInTheDocument()
  })

  it('shows parsing badge for PARSING status', () => {
    const version = createVersion({ parseStatus: 'PARSING' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.parseStatus.parsing')).toBeInTheDocument()
  })

  it('shows failed badge for FAILED status', () => {
    const version = createVersion({ parseStatus: 'FAILED' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.parseStatus.failed')).toBeInTheDocument()
  })

  it('shows no status badge for NOT_APPLICABLE', () => {
    const version = createVersion({ parseStatus: 'NOT_APPLICABLE' })

    const { container } = render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    // Should not show any of the status texts
    expect(container.querySelector('[data-variant="default"]')).not.toBeInTheDocument()
  })

  it('shows archived badge when status is ARCHIVED', () => {
    const version = createVersion({ status: 'ARCHIVED' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.timeline.archived')).toBeInTheDocument()
  })

  it('shows type badge for ORIGINAL', () => {
    const version = createVersion({ versionType: 'ORIGINAL' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.versionDetail.original')).toBeInTheDocument()
  })

  it('shows type badge for CONVERTED', () => {
    const version = createVersion({ versionType: 'CONVERTED' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.versionDetail.converted')).toBeInTheDocument()
  })

  it('shows type badge for AI_OPTIMIZED', () => {
    const version = createVersion({ versionType: 'AI_OPTIMIZED' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.versionDetail.aiOptimized')).toBeInTheDocument()
  })

  it('calls onEdit when edit button clicked', () => {
    const version = createVersion({
      versionType: 'CONVERTED',
      status: 'ACTIVE',
    })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    fireEvent.click(screen.getByText('resume.versionDetail.editContent'))
    expect(mockOnEdit).toHaveBeenCalled()
  })

  it('calls onCreateCopy when create copy button clicked', () => {
    const version = createVersion()

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onCreateCopy={mockOnCreateCopy}
      />
    )

    fireEvent.click(screen.getByText('resume.versionDetail.createCopy'))
    expect(mockOnCreateCopy).toHaveBeenCalled()
  })

  it('calls onActivate when activate button clicked', () => {
    const version = createVersion({
      versionType: 'CONVERTED',
      status: 'ARCHIVED',
    })

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onCreateCopy={mockOnCreateCopy}
        onActivate={mockOnActivate}
      />
    )

    fireEvent.click(screen.getByText('resume.versionDetail.setActive'))
    expect(mockOnActivate).toHaveBeenCalled()
  })

  it('shows activate button only for archived non-original versions', () => {
    const version = createVersion({
      versionType: 'CONVERTED',
      status: 'ARCHIVED',
    })

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onActivate={mockOnActivate}
      />
    )

    expect(screen.getByText('resume.versionDetail.setActive')).toBeInTheDocument()
  })

  it('does not show activate button for original versions', () => {
    const version = createVersion({
      versionType: 'ORIGINAL',
      status: 'ARCHIVED',
    })

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onActivate={mockOnActivate}
      />
    )

    expect(screen.queryByText('resume.versionDetail.setActive')).not.toBeInTheDocument()
  })

  it('does not show activate button for active versions', () => {
    const version = createVersion({
      versionType: 'CONVERTED',
      status: 'ACTIVE',
    })

    render(
      <VersionDetail
        version={version}
        onEdit={mockOnEdit}
        onActivate={mockOnActivate}
      />
    )

    expect(screen.queryByText('resume.versionDetail.setActive')).not.toBeInTheDocument()
  })

  it('does not show edit button for original versions', () => {
    const version = createVersion({
      versionType: 'ORIGINAL',
      status: 'ACTIVE',
    })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.queryByText('resume.versionDetail.editContent')).not.toBeInTheDocument()
  })

  it('renders parsed content sections when available', () => {
    const version = createVersion({
      parsedContent: {
        title: 'Senior Dev',
        company: 'TechCorp',
        requirements: ['React', 'TypeScript'],
        description: 'Build great UI',
      },
    })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('Senior Dev')).toBeInTheDocument()
    expect(screen.getByText('TechCorp')).toBeInTheDocument()
    expect(screen.getByText('Build great UI')).toBeInTheDocument()
    expect(screen.getByText('React')).toBeInTheDocument()
    expect(screen.getByText('TypeScript')).toBeInTheDocument()
  })

  it('renders not specified for missing parsed content fields', () => {
    const version = createVersion({
      parsedContent: {
        title: '',
        company: '',
        requirements: [],
        description: '',
      },
    })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    const notSpecifiedTexts = screen.getAllByText('common.notSpecified')
    expect(notSpecifiedTexts.length).toBeGreaterThanOrEqual(2)
  })

  it('renders raw content section', () => {
    const version = createVersion({ content: 'Raw markdown content' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('Raw markdown content')).toBeInTheDocument()
  })

  it('renders content not available when content is empty', () => {
    const version = createVersion({ content: '' })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.getByText('resume.versionDetail.contentNotAvailable')).toBeInTheDocument()
  })

  it('renders download button with correct props', () => {
    const version = createVersion({
      versionType: 'CONVERTED',
      versionId: 'v2',
    })

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    const downloadBtn = screen.getByTestId('download-btn')
    expect(downloadBtn).toHaveAttribute('data-version', 'v2')
    expect(downloadBtn).toHaveAttribute('data-filename', 'resume-converted')
  })

  it('does not show create copy button when onCreateCopy not provided', () => {
    const version = createVersion()

    render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(screen.queryByText('resume.versionDetail.createCopy')).not.toBeInTheDocument()
  })

  it('does not show parsed content grid when parsedContent is null', () => {
    const version = createVersion({ parsedContent: undefined })

    const { container } = render(
      <VersionDetail version={version} onEdit={mockOnEdit} />
    )

    expect(container.querySelector('.grid')).not.toBeInTheDocument()
  })
})
