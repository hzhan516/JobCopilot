import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import TrackingPage from './Tracking'

vi.mock('react-i18next', () =>> ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useSearchParams: () => [new URLSearchParams(), vi.fn()],
  }
})

vi.mock('@/utils/i18n', () => ({
  formatDate: (date: string) => date || '',
  formatDateTime: (date: string) => date || '',
}))

const mockGetTrackings = vi.fn()
const mockGetTrackingStats = vi.fn()
const mockCreateTracking = vi.fn()
const mockUpdateTracking = vi.fn()
const mockDeleteTracking = vi.fn()

vi.mock('@/services/trackingService', () => ({
  trackingService: {
    getTrackings: () => mockGetTrackings(),
    getTrackingStats: () => mockGetTrackingStats(),
    createTracking: (data: any) => mockCreateTracking(data),
    updateTracking: (id: string, data: any) => mockUpdateTracking(id, data),
    deleteTracking: (id: string) => mockDeleteTracking(id),
  },
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, variant, className, size }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('@/components/ui/badge', () => ({
  Badge: ({ children, className }: any) => <span className={className}>{children}</span>,
}))

vi.mock('@/components/ui/skeleton', () => ({
  Skeleton: ({ className }: any) => <div data-testid="skeleton" className={className} />,
}))

vi.mock('@/components/ui/input', () => ({
  Input: ({ value, onChange, placeholder, type, max }: any) => (
    <input value={value || ''} onChange={onChange} placeholder={placeholder} type={type} max={max} />
  ),
}))

vi.mock('@/components/ui/label', () => ({
  Label: ({ children }: any) => <label>{children}</label>,
}))

vi.mock('@/components/ui/dialog', () => ({
  Dialog: ({ children, open, onOpenChange }: any) => (
    <div data-testid="dialog" data-open={open}>{children}</div>
  ),
  DialogContent: ({ children }: any) => <div>{children}</div>,
  DialogHeader: ({ children }: any) => <div>{children}</div>,
  DialogTitle: ({ children }: any) => <div>{children}</div>,
  DialogDescription: ({ children }: any) => <div>{children}</div>,
  DialogFooter: ({ children }: any) => <div>{children}</div>,
}))

vi.mock('@/components/ui/select', () => ({
  Select: ({ children, value, onValueChange }: any) => (
    <div data-testid="select" data-value={value}>
      {children}
    </div>
  ),
  SelectTrigger: ({ children }: any) => <button>{children}</button>,
  SelectContent: ({ children }: any) => <div>{children}</div>,
  SelectItem: ({ children, value }: any) => (
    <button onClick={() => { /* select handler would be called */ }}>{children}</button>
  ),
  SelectValue: () => <span>Select</span>,
}))

vi.mock('@/components/ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children, asChild }: any) => children,
  DropdownMenuContent: ({ children, align }: any) => <div>{children}</div>,
  DropdownMenuItem: ({ children, onClick, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('lucide-react', () => ({
  ClipboardList: () => <span>📋</span>,
  Plus: () => <span>➕</span>,
  Building2: () => <span>🏢</span>,
  Briefcase: () => <span>💼</span>,
  Calendar: () => <span>📅</span>,
  Clock: () => <span>🕐</span>,
  MoreHorizontal: () => <span>⋯</span>,
  Pencil: () => <span>✏️</span>,
  Trash2: () => <span>🗑️</span>,
}))

describe('Tracking page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetTrackings.mockReset()
    mockGetTrackingStats.mockReset()
    mockCreateTracking.mockReset()
    mockUpdateTracking.mockReset()
    mockDeleteTracking.mockReset()
  })

  it('renders skeleton while loading', () => {
    mockGetTrackings.mockImplementation(() => new Promise(() => {}))

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0)
  })

  it('renders empty state when no trackings', async () => {
    mockGetTrackings.mockResolvedValue([])
    mockGetTrackingStats.mockResolvedValue(null)

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('tracking.emptyTitle')).toBeInTheDocument()
      expect(screen.getByText('tracking.emptyDesc')).toBeInTheDocument()
    })
  })

  it('fetches trackings and stats on mount', () => {
    mockGetTrackings.mockResolvedValue([])
    mockGetTrackingStats.mockResolvedValue(null)

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    expect(mockGetTrackings).toHaveBeenCalled()
    expect(mockGetTrackingStats).toHaveBeenCalled()
  })

  it('renders tracking records with status badges', async () => {
    mockGetTrackings.mockResolvedValue([
      {
        trackingId: 't1',
        jobTitle: 'Frontend Dev',
        companyName: 'TechCorp',
        status: 'APPLIED',
        appliedAt: '2024-01-15',
        notes: 'Applied via referral',
        createdAt: '2024-01-15',
        updatedAt: '2024-01-15',
      },
      {
        trackingId: 't2',
        jobTitle: 'Backend Dev',
        companyName: 'CloudInc',
        status: 'INTERVIEWING',
        appliedAt: '2024-01-10',
        notes: null,
        createdAt: '2024-01-10',
        updatedAt: '2024-01-20',
      },
    ])
    mockGetTrackingStats.mockResolvedValue({ applied: 1, interview: 1, offer: 0, total: 2, successRate: 0 })

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('Frontend Dev')).toBeInTheDocument()
      expect(screen.getByText('TechCorp')).toBeInTheDocument()
      expect(screen.getByText('Backend Dev')).toBeInTheDocument()
    })
  })

  it('opens add dialog on button click', async () => {
    mockGetTrackings.mockResolvedValue([])
    mockGetTrackingStats.mockResolvedValue(null)

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('tracking.addRecord')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('tracking.addRecord'))
    expect(screen.getByTestId('dialog')).toHaveAttribute('data-open', 'true')
  })

  it('shows stats cards', async () => {
    mockGetTrackings.mockResolvedValue([])
    mockGetTrackingStats.mockResolvedValue({ applied: 5, interview: 2, offer: 1, total: 8, successRate: 12.5 })

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument()
      expect(screen.getByText('2')).toBeInTheDocument()
      expect(screen.getByText('1')).toBeInTheDocument()
      expect(screen.getByText('8')).toBeInTheDocument()
    })
  })

  it('uses local counts when stats API fails', async () => {
    mockGetTrackings.mockResolvedValue([
      { trackingId: 't1', jobTitle: 'Job', companyName: 'Company', status: 'APPLIED', createdAt: '2024-01-01', updatedAt: '2024-01-01' },
    ])
    mockGetTrackingStats.mockRejectedValue(new Error('Stats unavailable'))

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument()
    })
  })

  it('handles status update', async () => {
    mockGetTrackings.mockResolvedValue([
      {
        trackingId: 't1',
        jobTitle: 'Frontend Dev',
        companyName: 'TechCorp',
        status: 'APPLIED',
        appliedAt: '2024-01-15',
        createdAt: '2024-01-15',
        updatedAt: '2024-01-15',
      },
    ])
    mockGetTrackingStats.mockResolvedValue(null)
    mockUpdateTracking.mockResolvedValue({})

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('Frontend Dev')).toBeInTheDocument()
    })
  })

  it('handles delete tracking', async () => {
    mockGetTrackings.mockResolvedValue([
      {
        trackingId: 't1',
        jobTitle: 'Frontend Dev',
        companyName: 'TechCorp',
        status: 'APPLIED',
        createdAt: '2024-01-15',
        updatedAt: '2024-01-15',
      },
    ])
    mockGetTrackingStats.mockResolvedValue(null)
    mockDeleteTracking.mockResolvedValue({})

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('Frontend Dev')).toBeInTheDocument()
    })

    // Dropdown delete button
    const deleteButtons = screen.getAllByText('🗑️')
    fireEvent.click(deleteButtons[0])

    await waitFor(() => {
      expect(mockDeleteTracking).toHaveBeenCalledWith('t1')
      expect(toast.success).toHaveBeenCalledWith('tracking.deleteSuccess')
    })
  })

  it('shows error when load trackings fails', async () => {
    mockGetTrackings.mockRejectedValue(new Error('Load failed'))

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('tracking.loadError')
    })
  })

  it('shows success rate bar', async () => {
    mockGetTrackings.mockResolvedValue([])
    mockGetTrackingStats.mockResolvedValue({ applied: 10, interview: 5, offer: 2, total: 15, successRate: 13.3 })

    render(
      <MemoryRouter>
        <TrackingPage />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('tracking.successRate')).toBeInTheDocument()
    })
  })
})
