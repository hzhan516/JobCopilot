import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Dashboard from './Dashboard'

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

vi.mock('@/services/resumeService', () => ({
  resumeService: {
    getResumeGroups: vi.fn().mockResolvedValue([
      { title: 'Resume 1', convertedVersion: { versionId: 'v1', exists: true }, originalVersion: { versionId: 'v2' } },
    ]),
  },
}))

vi.mock('@/services/jobService', () => ({
  jobService: {
    getJobs: vi.fn().mockResolvedValue([{ id: 'job-1', parsedContent: { title: 'Dev', company: 'Corp' } }]),
    getScoreHistory: vi.fn().mockResolvedValue([
      { id: 's1', jobId: 'job-1', resumeVersionId: 'v1', skillScore: 0.8, experienceScore: 0.7, overallScore: 0.75 },
    ]),
  },
}))

vi.mock('@/services/trackingService', () => ({
  trackingService: {
    getTrackings: vi.fn().mockResolvedValue([
      {
        trackingId: 't1',
        jobTitle: 'Frontend Dev',
        companyName: 'TechCorp',
        status: 'APPLIED',
        appliedAt: '2024-01-01',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-02',
      },
    ]),
  },
}))

vi.mock('@/services/chatService', () => ({
  default: {
    getConversations: vi.fn().mockResolvedValue([{ conversationId: 'c1' }]),
  },
}))

vi.mock('@/utils/i18n', () => ({
  formatDate: (date: string) => date,
  formatDateTime: (date: string) => date,
}))

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders welcome banner with title and subtitle', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.welcome')).toBeInTheDocument()
      expect(screen.getByText('dashboard.subtitle')).toBeInTheDocument()
    })
  })

  it('renders stat cards after loading', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.stats.resumes')).toBeInTheDocument()
      expect(screen.getByText('dashboard.stats.matches')).toBeInTheDocument()
      expect(screen.getByText('dashboard.stats.applications')).toBeInTheDocument()
      expect(screen.getByText('dashboard.stats.chats')).toBeInTheDocument()
    })
  })

  it('displays stat values after data loads', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      // Should show "1" for resumes, jobs, trackings, and chats
      const values = screen.getAllByText('1')
      expect(values.length).toBeGreaterThanOrEqual(3)
    })
  })

  it('renders quick actions section', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.quickActions.title')).toBeInTheDocument()
      expect(screen.getByText('dashboard.quickActions.resumes')).toBeInTheDocument()
      expect(screen.getByText('dashboard.quickActions.jobs')).toBeInTheDocument()
      expect(screen.getByText('dashboard.quickActions.chat')).toBeInTheDocument()
      expect(screen.getByText('dashboard.quickActions.tracking')).toBeInTheDocument()
    })
  })

  it('navigates to resumes page on upload resume button click', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.uploadResume')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('dashboard.uploadResume'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes')
  })

  it('navigates to jobs page on view jobs button click', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.viewJobs')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('dashboard.viewJobs'))
    expect(mockNavigate).toHaveBeenCalledWith('/jobs')
  })

  it('renders recommended jobs section with score data', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.recommendedJobs.title')).toBeInTheDocument()
      expect(screen.getByText(/Dev/)).toBeInTheDocument()
      expect(screen.getByText(/Corp/)).toBeInTheDocument()
    })
  })

  it('renders application progress section with tracking data', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('dashboard.applicationProgress.title')).toBeInTheDocument()
      expect(screen.getByText('Frontend Dev')).toBeInTheDocument()
      expect(screen.getByText('TechCorp')).toBeInTheDocument()
    })
  })

  it('navigates to tracking edit page on tracking item click', async () => {
    render(
      <MemoryRouter>
        <Dashboard />
      </MemoryRouter>
    )

    await waitFor(() => {
      expect(screen.getByText('Frontend Dev')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('Frontend Dev'))
    expect(mockNavigate).toHaveBeenCalledWith('/applications?edit=' + encodeURIComponent('t1'))
  })
})
