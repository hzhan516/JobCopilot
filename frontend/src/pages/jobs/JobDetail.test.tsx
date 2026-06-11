import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import JobDetail from './JobDetail'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ jobId: 'job-1' }),
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => {
  const t = (key: string) => key

  return {
  useTranslation: () => ({
    t,
  }),
  }
})

const mockGetJob = vi.fn()
const mockGetResumeGroups = vi.fn()
const mockGetScoreHistory = vi.fn()
const mockUpdateJob = vi.fn()
const mockScoreJob = vi.fn()
const mockTrackAction = vi.fn()

vi.mock('@/services/jobService', () => ({
  jobService: {
    getJob: (id: string) => mockGetJob(id),
    getScoreHistory: () => mockGetScoreHistory(),
    updateJob: (id: string, data: any) => mockUpdateJob(id, data),
    scoreJob: (id: string, data: any) => mockScoreJob(id, data),
    trackAction: (id: string, action: string, versionId?: string) => mockTrackAction(id, action, versionId),
  },
}))

vi.mock('@/services/resumeService', () => ({
  resumeService: {
    getResumeGroups: () => mockGetResumeGroups(),
  },
}))

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
  },
}))

// Mock UI components
vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, disabled, variant, size, className, asChild }: any) => {
    if (asChild) return <>{children}</>
    return <button onClick={onClick} disabled={disabled} className={className}>{children}</button>
  },
}))

vi.mock('@/components/ui/card', () => ({
  Card: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardContent: ({ children, className }: any) => <div className={className}>{children}</div>,
  CardHeader: ({ children, className }: any) => <div className={className}>{children}</div>,
}))

vi.mock('@/components/ui/badge', () => ({
  Badge: ({ children, variant, className }: any) => <span className={className}>{children}</span>,
}))

vi.mock('@/components/ui/skeleton', () => ({
  Skeleton: ({ className }: any) => <div data-testid="skeleton" className={className} />,
}))

vi.mock('@/components/ui/input', () => ({
  Input: ({ value, onChange, placeholder, className, type, max }: any) => (
    <input
      value={value || ''}
      onChange={onChange}
      placeholder={placeholder}
      className={className}
      type={type}
      max={max}
    />
  ),
}))

vi.mock('@/components/ui/textarea', () => ({
  Textarea: ({ value, onChange, placeholder, rows }: any) => (
    <textarea value={value || ''} onChange={onChange} placeholder={placeholder} rows={rows} />
  ),
}))

vi.mock('lucide-react', () => ({
  ArrowLeft: () => <span>←</span>,
  Building2: () => <span>🏢</span>,
  ExternalLink: () => <span>🔗</span>,
  List: () => <span>📋</span>,
  FileText: () => <span>📄</span>,
  Sparkles: () => <span>✨</span>,
  Loader2: () => <span data-testid="loader">⏳</span>,
  Save: () => <span>💾</span>,
  X: () => <span>✕</span>,
  Pencil: () => <span>✏️</span>,
  Star: () => <span>⭐</span>,
  MapPin: () => <span>📍</span>,
  DollarSign: () => <span>💰</span>,
  Trash2: () => <span>🗑️</span>,
  Plus: () => <span>➕</span>,
}))

function renderJobDetail() {
  return render(
    <MemoryRouter initialEntries={['/jobs/job-1']}>
      <Routes>
        <Route path="/jobs/:jobId" element={<JobDetail />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('JobDetail page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetJob.mockReset()
    mockGetResumeGroups.mockReset()
    mockGetScoreHistory.mockReset()
    mockUpdateJob.mockReset()
    mockScoreJob.mockReset()
    mockTrackAction.mockReset()
    mockTrackAction.mockResolvedValue(undefined)
  })

  it('renders skeleton while loading', async () => {
    mockGetJob.mockImplementation(() => new Promise(() => {}))
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0)
  })

  it('renders not found when job fails to load', async () => {
    mockGetJob.mockRejectedValue(new Error('Not found'))
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.notFound')).toBeInTheDocument()
    })
  })

  it('renders job details when loaded', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      originalUrl: 'https://example.com/job',
      parsedContent: {
        title: 'Senior Frontend Dev',
        company: 'TechCorp',
        salary: '$120k-$150k',
        location: 'Remote',
        description: 'Build great UI',
        requirements: ['React', 'TypeScript'],
      },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('Senior Frontend Dev')).toBeInTheDocument()
      expect(screen.getByText('TechCorp')).toBeInTheDocument()
      expect(screen.getByText('Remote')).toBeInTheDocument()
      expect(screen.getByText('$120k-$150k')).toBeInTheDocument()
    })
  })

  it('navigates back to job list', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.backToList')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.backToList'))
    expect(mockNavigate).toHaveBeenCalledWith('/jobs')
  })

  it('enters edit mode and shows save/cancel buttons', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp', requirements: ['React'] },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.edit')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.edit'))

    expect(screen.getByText('common.cancel')).toBeInTheDocument()
    expect(screen.getByText('jobDetail.save')).toBeInTheDocument()
  })

  it('saves edited job and exits edit mode', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp', requirements: ['React'] },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])
    mockUpdateJob.mockResolvedValue({})

    const { toast } = await import('sonner')

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.edit')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.edit'))
    fireEvent.click(screen.getByText('jobDetail.save'))

    await waitFor(() => {
      expect(mockUpdateJob).toHaveBeenCalled()
      expect(toast.success).toHaveBeenCalledWith('jobDetail.saveSuccess')
    })
  })

  it('shows score result when history exists', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([
      {
        jobId: 'job-1',
        resumeVersionId: 'v1',
        suitable: true,
        summary: 'Good match',
        finalScore: 0.85,
        skillScore: 0.9,
        experienceScore: 0.8,
        overallScore: 0.85,
      },
    ])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('Good match')).toBeInTheDocument()
    })
  })

  it('handles score job with selected resume', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([
      {
        title: 'My Resume',
        convertedVersion: { versionId: 'v1', exists: true, parseStatus: 'COMPLETED' },
        aiOptimizedVersion: null,
        originalVersion: null,
      },
    ])
    mockGetScoreHistory.mockResolvedValue([])
    mockScoreJob.mockResolvedValue({
      suitable: true,
      summary: 'Great match',
      finalScore: 0.9,
      breakdown: { skillScore: 0.9, experienceScore: 0.9, overallScore: 0.9 },
    })

    const { toast } = await import('sonner')

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.startScore')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.startScore'))

    await waitFor(() => {
      expect(mockScoreJob).toHaveBeenCalledWith('job-1', { resumeVersionId: 'v1' })
      expect(toast.success).toHaveBeenCalledWith('jobDetail.scoreSuccess')
    })
  })

  it('disables score button when no resume is selected', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.startScore')).toBeInTheDocument()
    })

    expect(screen.getByText('jobDetail.startScore')).toBeDisabled()
  })

  it('handles track action APPLY', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])
    mockTrackAction.mockResolvedValue({})

    renderJobDetail()

    await waitFor(() => expect(screen.getByText('jobDetail.markAsApplied')).toBeInTheDocument())
    fireEvent.click(screen.getByText('jobDetail.markAsApplied').closest('button')!)

    await waitFor(() => {
      expect(mockTrackAction).toHaveBeenCalledWith('job-1', 'APPLY', undefined)
    })
  })

  it('tracks click action on mount', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(mockTrackAction).toHaveBeenCalledWith('job-1', 'CLICK', undefined)
    })
  })

  it('disables score button when job not completed', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'PENDING',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      const scoreBtn = screen.getByText('jobDetail.startScore')
      expect(scoreBtn).toBeDisabled()
    })
  })

  it('shows external link when originalUrl exists', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      originalUrl: 'https://example.com/job',
      parsedContent: { title: 'Dev', company: 'Corp' },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.viewSource')).toBeInTheDocument()
    })
  })

  it('adds and removes requirements in edit mode', async () => {
    mockGetJob.mockResolvedValue({
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Dev', company: 'Corp', requirements: ['React'] },
    })
    mockGetResumeGroups.mockResolvedValue([])
    mockGetScoreHistory.mockResolvedValue([])

    renderJobDetail()

    await waitFor(() => {
      expect(screen.getByText('jobDetail.edit')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.edit'))

    await waitFor(() => {
      expect(screen.getByText('jobDetail.addRequirement')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('jobDetail.addRequirement'))

    await waitFor(() => {
      expect(screen.getAllByPlaceholderText('jobDetail.requirementPlaceholder').length).toBe(2)
    })
  })
})
