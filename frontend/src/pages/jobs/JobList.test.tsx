import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import JobList from './JobList'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockFetchJobs = vi.fn()
const mockLoadScoreHistory = vi.fn()
const mockSetSearchQuery = vi.fn()
const mockSetSortBy = vi.fn()
const mockSetSelectedResume = vi.fn()
const mockScoreJob = vi.fn()
const mockSubmitJob = vi.fn()
const mockDeleteJob = vi.fn()

const createMockStore = (overrides = {}) => ({
  jobs: [
    {
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Frontend Dev', company: 'TechCorp' },
    },
    {
      id: 'job-2',
      status: 'PENDING',
      parsedContent: { title: 'Backend Dev', company: 'CloudInc' },
    },
  ],
  filteredJobs: [
    {
      id: 'job-1',
      status: 'COMPLETED',
      parsedContent: { title: 'Frontend Dev', company: 'TechCorp' },
    },
    {
      id: 'job-2',
      status: 'PENDING',
      parsedContent: { title: 'Backend Dev', company: 'CloudInc' },
    },
  ],
  loading: false,
  filters: { searchQuery: '', sortBy: 'date' as const },
  scoreResults: {},
  scoringState: {},
  selectedResumes: {},
  fetchJobs: mockFetchJobs,
  loadScoreHistory: mockLoadScoreHistory,
  setSearchQuery: mockSetSearchQuery,
  setSortBy: mockSetSortBy,
  setSelectedResume: mockSetSelectedResume,
  scoreJob: mockScoreJob,
  submitJob: mockSubmitJob,
  deleteJob: mockDeleteJob,
  ...overrides,
})

let mockStore = createMockStore()

vi.mock('@/store/job.store', () => ({
  useJobStore: (selector: any) => selector(mockStore),
}))

vi.mock('@/services/resumeService', () => ({
  resumeService: {
    getResumeGroups: vi.fn().mockResolvedValue([
      {
        title: 'My Resume',
        convertedVersion: { versionId: 'v1', exists: true, parseStatus: 'COMPLETED' },
        aiOptimizedVersion: { versionId: 'v2', exists: true, parseStatus: 'COMPLETED' },
      },
    ]),
  },
}))

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}))

// Mock child components
vi.mock('./components/JobListHeader', () => ({
  default: ({ title, subtitle, addButtonLabel, onAddClick }: any) => (
    <div>
      <h1>{title}</h1>
      <p>{subtitle}</p>
      <button onClick={onAddClick}>{addButtonLabel}</button>
    </div>
  ),
}))

vi.mock('./components/JobFilterBar', () => ({
  default: ({ searchQuery, sortBy, matchFilter, onSearchChange, onSortChange, onMatchFilterChange, searchPlaceholder, sortLabel, sortOptions, matchFilterLabel, matchFilterOptions }: any) => (
    <div>
      <input
        placeholder={searchPlaceholder}
        value={searchQuery}
        onChange={(e) => onSearchChange(e.target.value)}
      />
      <select value={sortBy} onChange={(e) => onSortChange(e.target.value)}>
        {sortOptions.map((opt: any) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      <select value={matchFilter} onChange={(e) => onMatchFilterChange(e.target.value)}>
        {matchFilterOptions.map((opt: any) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  ),
}))

vi.mock('./components/JobCard', () => ({
  default: ({ job, selectedResumeId, scoreResult, isScoring, onSelectResume, onScore, onViewDetail, onDelete }: any) => (
    <div data-testid={`job-card-${job.id}`}>
      <span>{job.parsedContent?.title}</span>
      <span>{job.parsedContent?.company}</span>
      <button onClick={onViewDetail}>View</button>
      <button onClick={onScore}>Score</button>
      <button onClick={onDelete}>Delete</button>
      <select value={selectedResumeId} onChange={(e) => onSelectResume(e.target.value)}>
        <option value="">Select resume</option>
        <option value="v1">Resume v1</option>
      </select>
    </div>
  ),
}))

vi.mock('./components/JobListSkeleton', () => ({
  default: ({ title, subtitle }: any) => (
    <div data-testid="job-skeleton">
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </div>
  ),
}))

vi.mock('./components/JobEmptyState', () => ({
  default: ({ title, description }: any) => (
    <div data-testid="empty-state">
      <p>{title}</p>
      <p>{description}</p>
    </div>
  ),
}))

vi.mock('./components/JobCreateModal', () => ({
  default: ({ open, onOpenChange, onSubmit }: any) => (
    <div data-testid="create-modal" data-open={open}>
      {open && (
        <button onClick={() => onSubmit('https://example.com/job', new File(['test'], 'screenshot.png'))}>
          Submit
        </button>
      )}
    </div>
  ),
}))

describe('JobList page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore = createMockStore()
  })

  it('renders job list with header and filters', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    expect(screen.getByText('jobList.title')).toBeInTheDocument()
    expect(screen.getByText('jobList.subtitle')).toBeInTheDocument()
  })

  it('fetches jobs and score history on mount', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    expect(mockFetchJobs).toHaveBeenCalled()
    expect(mockLoadScoreHistory).toHaveBeenCalled()
  })

  it('renders job cards for each job', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    expect(screen.getByTestId('job-card-job-1')).toBeInTheDocument()
    expect(screen.getByTestId('job-card-job-2')).toBeInTheDocument()
  })

  it('renders skeleton when loading and no jobs', () => {
    mockStore = createMockStore({ loading: true, jobs: [], filteredJobs: [] })

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    expect(screen.getByTestId('job-skeleton')).toBeInTheDocument()
  })

  it('renders empty state when no jobs', () => {
    mockStore = createMockStore({ loading: false, jobs: [], filteredJobs: [] })

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    expect(screen.getByTestId('empty-state')).toBeInTheDocument()
  })

  it('handles search query change', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const searchInput = screen.getByPlaceholderText('jobList.searchPlaceholder')
    fireEvent.change(searchInput, { target: { value: 'Frontend' } })

    expect(mockSetSearchQuery).toHaveBeenCalledWith('Frontend')
  })

  it('handles sort change', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const sortSelect = screen.getAllByRole('combobox')[0]
    fireEvent.change(sortSelect, { target: { value: 'status' } })

    expect(mockSetSortBy).toHaveBeenCalledWith('status')
  })

  it('handles match filter change', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const matchSelect = screen.getAllByRole('combobox')[1]
    fireEvent.change(matchSelect, { target: { value: 'HIGH' } })
    // matchFilter is local state, should trigger re-render with filtered jobs
  })

  it('opens create modal when clicking add button', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('jobList.addJob'))

    expect(screen.getByTestId('create-modal')).toHaveAttribute('data-open', 'true')
  })

  it('navigates to job detail on view click', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const viewButtons = screen.getAllByText('View')
    fireEvent.click(viewButtons[0])

    expect(mockNavigate).toHaveBeenCalledWith('/jobs/job-1')
  })

  it('handles job scoring', async () => {
    mockStore = createMockStore({
      selectedResumes: { 'job-1': 'v1' },
    })

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const scoreButtons = screen.getAllByText('Score')
    fireEvent.click(scoreButtons[0])

    await waitFor(() => {
      expect(mockScoreJob).toHaveBeenCalledWith('job-1', 'v1')
    })
  })

  it('shows error when scoring without selected resume', async () => {
    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const scoreButtons = screen.getAllByText('Score')
    fireEvent.click(scoreButtons[0])

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('jobList.selectResumeForScore')
    })
  })

  it('handles job deletion with confirmation', async () => {
    const originalConfirm = window.confirm
    window.confirm = vi.fn(() => true)

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const deleteButtons = screen.getAllByText('Delete')
    fireEvent.click(deleteButtons[0])

    await waitFor(() => {
      expect(mockDeleteJob).toHaveBeenCalledWith('job-1')
    })

    window.confirm = originalConfirm
  })

  it('cancels deletion when confirm is false', async () => {
    const originalConfirm = window.confirm
    window.confirm = vi.fn(() => false)

    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const deleteButtons = screen.getAllByText('Delete')
    fireEvent.click(deleteButtons[0])

    await waitFor(() => {
      expect(mockDeleteJob).not.toHaveBeenCalled()
    })

    window.confirm = originalConfirm
  })

  it('handles resume selection for a job', () => {
    render(
      <MemoryRouter>
        <JobList />
      </MemoryRouter>
    )

    const jobSelect = within(screen.getByTestId('job-card-job-1')).getByRole('combobox')
    fireEvent.change(jobSelect, { target: { value: 'v1' } })
    expect(mockSetSelectedResume).toHaveBeenCalledWith('job-1', 'v1')
  })
})
