import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ResumeList from './ResumeList'

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

const mockFetchGroups = vi.fn()
const mockUploadResume = vi.fn()
const mockPollParseStatus = vi.fn()
const mockDeleteGroup = vi.fn()

let mockStore = {
  groups: [] as any[],
  loading: false,
  fetchGroups: mockFetchGroups,
  uploadResume: mockUploadResume,
  pollParseStatus: mockPollParseStatus,
  deleteGroup: mockDeleteGroup,
}

vi.mock('@/store/resume.store', () => ({
  useResumeStore: (selector?: any) => typeof selector === 'function' ? selector(mockStore) : mockStore,
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/ui/dialog', () => ({
  Dialog: ({ children, open, onOpenChange }: any) => (
    <div data-testid="dialog" data-open={open}>{children}</div>
  ),
  DialogContent: ({ children }: any) => <div>{children}</div>,
  DialogHeader: ({ children }: any) => <div>{children}</div>,
  DialogTitle: ({ children }: any) => <div>{children}</div>,
  DialogDescription: ({ children }: any) => <div>{children}</div>,
}))

vi.mock('@/components/ui/spinner', () => ({
  Spinner: ({ className }: any) => <div data-testid="spinner" className={className} />,
}))

vi.mock('@/components/resume/ResumeCard', () => ({
  ResumeCard: ({ group, onView, onDelete }: any) => (
    <div data-testid={`resume-card-${group.groupId}`}>
      <span>{group.title}</span>
      <button onClick={() => onView(group.groupId)}>View</button>
      <button onClick={() => onDelete(group.groupId)}>Delete</button>
    </div>
  ),
}))

vi.mock('@/components/resume/ResumeUpload', () => ({
  ResumeUpload: ({ onUpload }: any) => (
    <div data-testid="resume-upload">
      <button onClick={() => onUpload(new File(['test'], 'resume.pdf'))}>Upload</button>
    </div>
  ),
}))

vi.mock('lucide-react', () => ({
  FileText: () => <span>📄</span>,
  Upload: () => <span>⬆️</span>,
}))

describe('ResumeList page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore = {
      groups: [],
      loading: false,
      fetchGroups: mockFetchGroups,
      uploadResume: mockUploadResume,
      pollParseStatus: mockPollParseStatus,
      deleteGroup: mockDeleteGroup,
    }
  })

  it('renders empty state when no resumes', () => {
    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    expect(screen.getByText('resume.list.noResumes')).toBeInTheDocument()
    expect(screen.getAllByText('resume.list.upload').length).toBeGreaterThan(0)
  })

  it('fetches resume groups on mount', () => {
    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    expect(mockFetchGroups).toHaveBeenCalled()
  })

  it('shows spinner while loading', () => {
    mockStore = { ...mockStore, loading: true, groups: [] }

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    expect(screen.getByTestId('spinner')).toBeInTheDocument()
  })

  it('renders resume cards', () => {
    mockStore = {
      ...mockStore,
      groups: [
        { groupId: 'g1', title: 'Resume 1' },
        { groupId: 'g2', title: 'Resume 2' },
      ],
    }

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    expect(screen.getByTestId('resume-card-g1')).toBeInTheDocument()
    expect(screen.getByTestId('resume-card-g2')).toBeInTheDocument()
  })

  it('navigates to resume detail on view click', () => {
    mockStore = {
      ...mockStore,
      groups: [{ groupId: 'g1', title: 'Resume 1' }],
    }

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('View'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes/g1')
  })

  it('deletes resume with confirmation', async () => {
    mockStore = {
      ...mockStore,
      groups: [{ groupId: 'g1', title: 'Resume 1' }],
    }
    mockDeleteGroup.mockResolvedValue({})

    const originalConfirm = window.confirm
    window.confirm = vi.fn(() => true)

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('Delete'))

    await waitFor(() => {
      expect(mockDeleteGroup).toHaveBeenCalledWith('g1')
    })

    window.confirm = originalConfirm
  })

  it('cancels delete when confirmation is false', async () => {
    mockStore = {
      ...mockStore,
      groups: [{ groupId: 'g1', title: 'Resume 1' }],
    }

    const originalConfirm = window.confirm
    window.confirm = vi.fn(() => false)

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByText('Delete'))

    await waitFor(() => {
      expect(mockDeleteGroup).not.toHaveBeenCalled()
    })

    window.confirm = originalConfirm
  })

  it('opens upload dialog on button click', () => {
    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getAllByText('resume.list.upload')[0])

    expect(screen.getByTestId('dialog')).toHaveAttribute('data-open', 'true')
  })

  it('handles upload success with completed parse status', async () => {
    mockUploadResume.mockResolvedValue({ groupId: 'g1' })
    mockPollParseStatus.mockResolvedValue('COMPLETED')

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByTestId('dialog').querySelector('button')!)

    await waitFor(() => {
      expect(mockUploadResume).toHaveBeenCalled()
      expect(toast.success).toHaveBeenCalledWith('resume.list.parseSuccess')
    })
  })

  it('handles upload with failed parse status', async () => {
    mockUploadResume.mockResolvedValue({ groupId: 'g1' })
    mockPollParseStatus.mockResolvedValue('FAILED')

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByTestId('dialog').querySelector('button')!)

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('resume.list.parseFailed')
    })
  })

  it('handles upload error', async () => {
    mockUploadResume.mockRejectedValue(new Error('Upload failed'))

    const { toast } = await import('sonner')

    render(
      <MemoryRouter>
        <ResumeList />
      </MemoryRouter>
    )

    fireEvent.click(screen.getByTestId('dialog').querySelector('button')!)

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('resume.list.uploadFailed')
    })
  })
})
