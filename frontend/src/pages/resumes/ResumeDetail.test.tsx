import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ResumeDetail from './ResumeDetail'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ groupId: 'group-1' }),
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/utils/i18n', () => ({
  formatDate: (date: string) => date,
  formatDateTime: (date: string) => date,
}))

const mockFetchGroupDetail = vi.fn()
const mockCreateVersion = vi.fn()
const mockActivateVersion = vi.fn()

let mockStore = {
  currentGroup: null as any,
  loading: false,
  fetchGroupDetail: mockFetchGroupDetail,
  createVersion: mockCreateVersion,
  activateVersion: mockActivateVersion,
}

vi.mock('@/store/resume.store.ts', () => ({
  useResumeStore: (selector: any) => selector(mockStore),
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, variant, size, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/resume/VersionTimeline', () => ({
  VersionTimeline: ({ versions, selectedVersionId, onSelectVersion, onActivateVersion }: any) => (
    <div data-testid="version-timeline">
      {versions.map((v: any) => (
        <div
          key={v.versionId}
          data-testid={`version-${v.versionId}`}
          data-selected={selectedVersionId === v.versionId}
          onClick={() => onSelectVersion(v.versionId)}
        >
          {v.versionType}
          {v.status === 'ARCHIVED' && v.versionType !== 'ORIGINAL' && (
            <button onClick={() => onActivateVersion(v.versionId)}>Activate</button>
          )}
        </div>
      ))}
    </div>
  ),
}))

vi.mock('@/components/resume/VersionDetail', () => ({
  VersionDetail: ({ version, onEdit, onCreateCopy, onActivate }: any) => (
    <div data-testid="version-detail">
      <span>{version.versionType}</span>
      <button onClick={onEdit}>Edit</button>
      <button onClick={onCreateCopy}>Create Copy</button>
      {onActivate && <button onClick={onActivate}>Activate</button>}
    </div>
  ),
}))

vi.mock('@/components/resume/AIOptimizeCompare', () => ({
  AIOptimizeCompare: ({ originalVersion, aiOptimizedVersion }: any) => (
    <div data-testid="ai-compare">
      <span data-testid="has-original">{originalVersion ? 'yes' : 'no'}</span>
      <span data-testid="has-ai">{aiOptimizedVersion ? 'yes' : 'no'}</span>
    </div>
  ),
}))

vi.mock('lucide-react', () => ({
  Loader2: () => <span>⏳</span>,
  ArrowLeft: () => <span>←</span>,
  Sparkles: () => <span>✨</span>,
  FileText: () => <span>📄</span>,
  Copy: () => <span>📋</span>,
}))

function renderResumeDetail() {
  return render(
    <MemoryRouter initialEntries={['/resumes/group-1']}>
      <Routes>
        <Route path="/resumes/:groupId" element={<ResumeDetail />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ResumeDetail page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore = {
      currentGroup: null,
      loading: false,
      fetchGroupDetail: mockFetchGroupDetail,
      createVersion: mockCreateVersion,
      activateVersion: mockActivateVersion,
    }
  })

  it('renders loader while loading', () => {
    mockStore = { ...mockStore, loading: true, currentGroup: null }

    renderResumeDetail()

    expect(screen.getByText('⏳')).toBeInTheDocument()
  })

  it('renders not found when no currentGroup', () => {
    mockStore = { ...mockStore, currentGroup: null, loading: false }

    renderResumeDetail()

    expect(screen.getByText('resume.detail.notFound')).toBeInTheDocument()
    expect(screen.getByText('resume.detail.backToList')).toBeInTheDocument()
  })

  it('fetches group detail on mount', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [],
      },
    }

    renderResumeDetail()

    expect(mockFetchGroupDetail).toHaveBeenCalledWith('group-1')
  })

  it('renders group title and version timeline', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
          { versionId: 'v2', versionType: 'CONVERTED', createdAt: '2024-01-02', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }

    renderResumeDetail()

    expect(screen.getByText('My Resume')).toBeInTheDocument()
    expect(screen.getByTestId('version-timeline')).toBeInTheDocument()
    expect(screen.getByTestId('version-v1')).toBeInTheDocument()
  })

  it('shows detail tab by default', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }

    renderResumeDetail()

    expect(screen.getByTestId('version-detail')).toBeInTheDocument()
  })

  it('shows compare tab when AI optimized version exists', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
          { versionId: 'v2', versionType: 'AI_OPTIMIZED', createdAt: '2024-01-02', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }

    renderResumeDetail()

    expect(screen.getByText('resume.compare.title')).toBeInTheDocument()
  })

  it('switches to compare tab', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
          { versionId: 'v2', versionType: 'AI_OPTIMIZED', createdAt: '2024-01-02', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }

    renderResumeDetail()

    fireEvent.click(screen.getByText('resume.compare.title'))
    expect(screen.getByTestId('ai-compare')).toBeInTheDocument()
  })

  it('navigates to edit page', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }

    renderResumeDetail()

    fireEvent.click(screen.getByText('Edit'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes/group-1/versions/v1/edit')
  })

  it('creates copy and navigates to edit', async () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }
    mockCreateVersion.mockResolvedValue('v2')

    const { toast } = await import('sonner')

    renderResumeDetail()

    fireEvent.click(screen.getByText('Create Copy'))

    await waitFor(() => {
      expect(mockCreateVersion).toHaveBeenCalledWith('group-1', 'v1')
      expect(toast.success).toHaveBeenCalledWith('resume.detail.createCopySuccess')
      expect(mockNavigate).toHaveBeenCalledWith('/resumes/group-1/versions/v2/edit')
    })
  })

  it('activates archived version from timeline', async () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
          { versionId: 'v2', versionType: 'CONVERTED', createdAt: '2024-01-02', status: 'ARCHIVED', parseStatus: 'COMPLETED' },
        ],
      },
    }
    mockActivateVersion.mockResolvedValue({})

    const { toast } = await import('sonner')

    renderResumeDetail()

    fireEvent.click(screen.getByText('Activate'))

    await waitFor(() => {
      expect(mockActivateVersion).toHaveBeenCalledWith('v2')
      expect(toast.success).toHaveBeenCalledWith('resume.detail.activateSuccess')
    })
  })

  it('shows manual edit button and creates copy', async () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', createdAt: '2024-01-01', status: 'ACTIVE', parseStatus: 'COMPLETED' },
        ],
      },
    }
    mockCreateVersion.mockResolvedValue('v2')

    renderResumeDetail()

    expect(screen.getByText('resume.detail.manualEdit')).toBeInTheDocument()

    fireEvent.click(screen.getByText('resume.detail.manualEdit'))

    await waitFor(() => {
      expect(mockCreateVersion).toHaveBeenCalledWith('group-1', undefined)
    })
  })

  it('navigates back to list', () => {
    mockStore = {
      ...mockStore,
      currentGroup: null,
      loading: false,
    }

    renderResumeDetail()

    fireEvent.click(screen.getByText('resume.detail.backToList'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes')
  })

  it('shows select version prompt when no version selected', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        createdAt: '2024-01-01',
        versions: [],
      },
    }

    renderResumeDetail()

    expect(screen.getByText('resume.detail.selectVersion')).toBeInTheDocument()
  })
})
