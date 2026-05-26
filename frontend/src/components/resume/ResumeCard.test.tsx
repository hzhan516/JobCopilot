import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ResumeCard } from './ResumeCard'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/utils/i18n', () => ({
  formatDate: () => 'Jan 15, 2024',
}))

vi.mock('../ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardDescription: ({ children }: any) => <p>{children}</p>,
  CardFooter: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardTitle: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('../ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => <span className={className}>{children}</span>,
}))

vi.mock('../ui/button', () => ({
  Button: ({ children, onClick, variant, size, className, title }: any) => (
    <button onClick={onClick} className={className} title={title}>{children}</button>
  ),
}))

vi.mock('./ParseStatusBadge', () => ({
  ParseStatusBadge: ({ status }: any) => <span data-testid={`parse-status-${status}`}>{status}</span>,
}))

vi.mock('lucide-react', () => ({
  FileText: () => <span>📄</span>,
  Trash2: () => <span>🗑️</span>,
  Eye: () => <span>👁️</span>,
}))

const createGroup = (overrides = {}) => ({
  groupId: 'g1',
  title: 'My Resume',
  createdAt: '2024-01-15T00:00:00Z',
  isDefault: false,
  versions: [
    { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE', parseStatus: 'COMPLETED' },
  ],
  ...overrides,
})

describe('ResumeCard', () => {
  const mockOnView = vi.fn()
  const mockOnDelete = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders resume title and creation date', () => {
    const group = createGroup()

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByText('My Resume')).toBeInTheDocument()
    expect(screen.getByText(/resume.card.created/)).toBeInTheDocument()
  })

  it('shows default badge when isDefault is true', () => {
    const group = createGroup({ isDefault: true })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByText('resume.card.default')).toBeInTheDocument()
  })

  it('does not show default badge when isDefault is false', () => {
    const group = createGroup({ isDefault: false })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.queryByText('resume.card.default')).not.toBeInTheDocument()
  })

  it('displays version count', () => {
    const group = createGroup({
      versions: [
        { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        { versionId: 'v2', versionType: 'CONVERTED', createdAt: '2024-01-16T00:00:00Z', status: 'ACTIVE', parseStatus: 'COMPLETED' },
      ],
    })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('shows latest parse status when available', () => {
    const group = createGroup({
      versions: [
        { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE', parseStatus: 'COMPLETED' },
      ],
    })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByTestId('parse-status-COMPLETED')).toBeInTheDocument()
  })

  it('hides parse status when NOT_APPLICABLE', () => {
    const group = createGroup({
      versions: [
        { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE', parseStatus: 'NOT_APPLICABLE' },
      ],
    })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.queryByTestId('parse-status-NOT_APPLICABLE')).not.toBeInTheDocument()
  })

  it('handles empty versions array', () => {
    const group = createGroup({ versions: [] })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByText('0')).toBeInTheDocument()
  })

  it('calls onView with groupId when view button clicked', () => {
    const group = createGroup()

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    fireEvent.click(screen.getByText('resume.card.viewDetails'))
    expect(mockOnView).toHaveBeenCalledWith('g1')
  })

  it('calls onDelete with groupId when delete button clicked', () => {
    const group = createGroup()

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    const deleteBtn = screen.getByTitle('resume.card.deleteTitle')
    fireEvent.click(deleteBtn)
    expect(mockOnDelete).toHaveBeenCalledWith('g1')
  })

  it('renders with long title truncated', () => {
    const group = createGroup({ title: 'A very long resume title that should be truncated' })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    expect(screen.getByText('A very long resume title that should be truncated')).toBeInTheDocument()
  })

  it('identifies latest version by createdAt', () => {
    const group = createGroup({
      versions: [
        { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-15T00:00:00Z', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        { versionId: 'v2', versionType: 'CONVERTED', createdAt: '2024-01-20T00:00:00Z', status: 'ACTIVE', parseStatus: 'PARSING' },
      ],
    })

    render(<ResumeCard group={group} onView={mockOnView} onDelete={mockOnDelete} />)

    // Should show PARSING status of v2 (latest by date)
    expect(screen.getByTestId('parse-status-PARSING')).toBeInTheDocument()
  })
})
