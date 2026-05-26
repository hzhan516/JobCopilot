import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import ResumeEdit from './ResumeEdit'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useParams: () => ({ groupId: 'group-1', versionId: 'v1' }),
    useNavigate: () => mockNavigate,
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockFetchGroupDetail = vi.fn()
const mockSaveVersion = vi.fn()

let mockStore = {
  currentGroup: null as any,
  loading: false,
  fetchGroupDetail: mockFetchGroupDetail,
  saveVersion: mockSaveVersion,
}

vi.mock('@/store/resume.store.ts', () => ({
  useResumeStore: (selector?: any) => typeof selector === 'function' ? selector(mockStore) : mockStore,
}))

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
  },
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, variant, size, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('@/components/resume/MarkdownEditor', () => ({
  MarkdownEditor: ({ initialContent, versionId, onSave, onCancel, onAutoSave, readOnly }: any) => (
    <div data-testid="markdown-editor">
      <span data-testid="editor-content">{initialContent}</span>
      <span data-testid="editor-version">{versionId}</span>
      <span data-testid="editor-readonly">{readOnly ? 'true' : 'false'}</span>
      <button onClick={() => onSave('updated content')}>Save</button>
      <button onClick={onCancel}>Cancel</button>
      {onAutoSave && <button onClick={() => onAutoSave('auto-saved')}>AutoSave</button>}
    </div>
  ),
}))

vi.mock('lucide-react', () => ({
  Loader2: () => <span>⏳</span>,
  ArrowLeft: () => <span>←</span>,
}))

function renderResumeEdit() {
  return render(
    <MemoryRouter initialEntries={['/resumes/group-1/versions/v1/edit']}>
      <Routes>
        <Route path="/resumes/:groupId/versions/:versionId/edit" element={<ResumeEdit />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ResumeEdit page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore = {
      currentGroup: null,
      loading: false,
      fetchGroupDetail: mockFetchGroupDetail,
      saveVersion: mockSaveVersion,
    }
  })

  it('renders loader while loading', () => {
    mockStore = { ...mockStore, loading: true, currentGroup: null }

    renderResumeEdit()

    expect(screen.getByText('⏳')).toBeInTheDocument()
  })

  it('renders not found when no currentGroup', () => {
    mockStore = { ...mockStore, currentGroup: null, loading: false }

    renderResumeEdit()

    expect(screen.getByText('resume.detail.notFound')).toBeInTheDocument()
  })

  it('renders version not found when version missing', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v2', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Other version' },
        ],
      },
      loading: false,
    }

    renderResumeEdit()

    expect(screen.getByText('resume.detail.versionNotFound')).toBeInTheDocument()
  })

  it('fetches group detail on mount', () => {
    mockStore = {
      ...mockStore,
      currentGroup: null,
    }

    renderResumeEdit()

    expect(mockFetchGroupDetail).toHaveBeenCalledWith('group-1')
  })

  it('renders markdown editor with version content', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'My resume text' },
        ],
      },
    }

    renderResumeEdit()

    expect(screen.getByTestId('markdown-editor')).toBeInTheDocument()
    expect(screen.getByTestId('editor-content')).toHaveTextContent('My resume text')
    expect(screen.getByTestId('editor-version')).toHaveTextContent('v1')
  })

  it('editor is readOnly when version is not ACTIVE', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ARCHIVED', status: 'ARCHIVED', content: 'Old content' },
        ],
      },
    }

    renderResumeEdit()

    expect(screen.getByTestId('editor-readonly')).toHaveTextContent('true')
  })

  it('editor is editable when version is ACTIVE', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Content' },
        ],
      },
    }

    renderResumeEdit()

    expect(screen.getByTestId('editor-readonly')).toHaveTextContent('false')
  })

  it('handles save and navigates back', async () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Content' },
        ],
      },
    }
    mockSaveVersion.mockResolvedValue({})

    const { toast } = await import('sonner')

    renderResumeEdit()

    fireEvent.click(screen.getByText('Save'))

    await waitFor(() => {
      expect(mockSaveVersion).toHaveBeenCalledWith('v1', 'updated content')
      expect(toast.success).toHaveBeenCalledWith('resume.markdownEditor.autoSaveSuccess')
      expect(mockNavigate).toHaveBeenCalledWith('/resumes/group-1')
    })
  })

  it('handles auto save without navigation', async () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Content' },
        ],
      },
    }
    mockSaveVersion.mockResolvedValue({})

    renderResumeEdit()

    fireEvent.click(screen.getByText('AutoSave'))

    await waitFor(() => {
      expect(mockSaveVersion).toHaveBeenCalledWith('v1', 'auto-saved')
    })
  })

  it('cancel navigates back to resume detail', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Content' },
        ],
      },
    }

    renderResumeEdit()

    fireEvent.click(screen.getByText('Cancel'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes/group-1')
  })

  it('shows group title and version type in header', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ORIGINAL', status: 'ACTIVE', content: 'Content' },
        ],
      },
    }

    renderResumeEdit()

    expect(screen.getByText('resume.edit.title')).toBeInTheDocument()
    expect(screen.getByText('My Resume - ORIGINAL')).toBeInTheDocument()
  })

  it('does not autoSave for non-ACTIVE versions', () => {
    mockStore = {
      ...mockStore,
      currentGroup: {
        groupId: 'group-1',
        title: 'My Resume',
        versions: [
          { versionId: 'v1', versionType: 'ARCHIVED', status: 'ARCHIVED', content: 'Content' },
        ],
      },
    }

    renderResumeEdit()

    expect(screen.queryByText('AutoSave')).not.toBeInTheDocument()
  })

  it('navigates to resumes list from not found', () => {
    mockStore = { ...mockStore, currentGroup: null, loading: false }

    renderResumeEdit()

    fireEvent.click(screen.getByText('resume.edit.back'))
    expect(mockNavigate).toHaveBeenCalledWith('/resumes')
  })
})
