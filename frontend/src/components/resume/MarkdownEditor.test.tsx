import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MarkdownEditor } from './MarkdownEditor'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockOnSave = vi.fn()
const mockOnCancel = vi.fn()
const mockOnAutoSave = vi.fn()

vi.mock('@uiw/react-md-editor', () => ({
  default: ({ value, onChange, preview, height, className, visibleDragbar, hideToolbar, textareaProps }: any) => (
    <div data-testid="md-editor" className={className}>
      <textarea
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
        data-preview={preview}
        data-height={height}
        data-visible-dragbar={visibleDragbar}
        data-hide-toolbar={hideToolbar}
        data-readonly={textareaProps?.readOnly}
        data-disabled={textareaProps?.disabled}
      />
    </div>
  ),
}))

vi.mock('../ui/button', () => ({
  Button: ({ children, onClick, variant, disabled }: any) => (
    <button onClick={onClick} disabled={disabled}>{children}</button>
  ),
}))

vi.mock('lucide-react', () => ({
  Save: () => <span>💾</span>,
  X: () => <span>✕</span>,
  Loader2: () => <span data-testid="loader-icon">⏳</span>,
}))

describe('MarkdownEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renders with initial content', () => {
    render(
      <MarkdownEditor
        initialContent="Initial markdown"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    expect(textarea).toHaveValue('Initial markdown')
  })

  it('shows read only badge when readOnly prop is true', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={true}
      />
    )

    expect(screen.getByText('resume.markdownEditor.readOnly')).toBeInTheDocument()
  })

  it('does not show read only badge when readOnly is false', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={false}
      />
    )

    expect(screen.queryByText('resume.markdownEditor.readOnly')).not.toBeInTheDocument()
  })

  it('disables save button when readOnly', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={true}
      />
    )

    const saveBtn = screen.getByText('resume.markdownEditor.save')
    expect(saveBtn).toBeDisabled()
  })

  it('calls onSave with content when save button clicked', () => {
    render(
      <MarkdownEditor
        initialContent="Test content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    fireEvent.click(screen.getByText('resume.markdownEditor.save'))
    expect(mockOnSave).toHaveBeenCalledWith('Test content')
  })

  it('calls onCancel when cancel button clicked', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    fireEvent.click(screen.getByText('resume.markdownEditor.cancel'))
    expect(mockOnCancel).toHaveBeenCalled()
  })

  it('loads from localStorage when autosave exists', () => {
    localStorage.setItem('resume-editor-autosave-v1', 'autosaved content')

    render(
      <MarkdownEditor
        initialContent="Initial"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    expect(textarea).toHaveValue('autosaved content')
  })

  it('does not load from localStorage when readOnly', () => {
    localStorage.setItem('resume-editor-autosave-v1', 'autosaved content')

    render(
      <MarkdownEditor
        initialContent="Initial"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={true}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    expect(textarea).toHaveValue('Initial')
  })

  it('sets editor to readOnly mode', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={true}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    expect(textarea).toHaveAttribute('data-readonly', 'true')
    expect(textarea).toHaveAttribute('data-disabled', 'true')
    expect(textarea).toHaveAttribute('data-hide-toolbar', 'true')
  })

  it('triggers auto save after debounce', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    mockOnAutoSave.mockResolvedValue(undefined)

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'Updated content' } })

    // Wait for debounce
    vi.advanceTimersByTime(4000)

    await waitFor(() => {
      expect(mockOnAutoSave).toHaveBeenCalledWith('Updated content')
    })

    vi.useRealTimers()
  })

  it('shows auto save success indicator', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    mockOnAutoSave.mockResolvedValue(undefined)

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'New content' } })

    vi.advanceTimersByTime(4000)

    await waitFor(() => {
      expect(screen.getByText('resume.markdownEditor.autoSaveSuccess')).toBeInTheDocument()
    })

    vi.useRealTimers()
  })

  it('shows auto save error indicator on failure', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    mockOnAutoSave.mockRejectedValue(new Error('Save failed'))

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'New content' } })

    vi.advanceTimersByTime(4000)

    await waitFor(() => {
      expect(screen.getByText('resume.markdownEditor.autoSaveError')).toBeInTheDocument()
    })

    vi.useRealTimers()
  })

  it('clears localStorage on save', () => {
    localStorage.setItem('resume-editor-autosave-v1', 'saved')

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    fireEvent.click(screen.getByText('resume.markdownEditor.save'))
    expect(localStorage.getItem('resume-editor-autosave-v1')).toBeNull()
  })

  it('clears localStorage on cancel', () => {
    localStorage.setItem('resume-editor-autosave-v1', 'saved')

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    fireEvent.click(screen.getByText('resume.markdownEditor.cancel'))
    expect(localStorage.getItem('resume-editor-autosave-v1')).toBeNull()
  })

  it('does not auto save when no onAutoSave prop', () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'Updated' } })

    vi.advanceTimersByTime(4000)

    expect(mockOnAutoSave).not.toHaveBeenCalled()

    vi.useRealTimers()
  })

  it('does not auto save when readOnly', () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
        readOnly={true}
      />
    )

    vi.advanceTimersByTime(4000)

    expect(mockOnAutoSave).not.toHaveBeenCalled()

    vi.useRealTimers()
  })

  it('does not save empty content', () => {
    render(
      <MarkdownEditor
        initialContent="   "
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    )

    fireEvent.click(screen.getByText('resume.markdownEditor.save'))
    expect(mockOnSave).not.toHaveBeenCalled()
  })

  it('shows saving indicator during auto save', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    mockOnAutoSave.mockImplementation(() => new Promise(() => {}))

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'New' } })

    vi.advanceTimersByTime(3500)

    await waitFor(() => {
      expect(screen.getByText('resume.markdownEditor.autoSaving')).toBeInTheDocument()
    })

    vi.useRealTimers()
  })

  it('shows conflict indicator on 409 error', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })

    const conflictError = {
      isAxiosError: true,
      response: { status: 409 },
    }
    mockOnAutoSave.mockRejectedValue(conflictError)

    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onAutoSave={mockOnAutoSave}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'New' } })

    vi.advanceTimersByTime(4000)

    await waitFor(() => {
      expect(screen.getByText('resume.markdownEditor.autoSaveConflict')).toBeInTheDocument()
    })

    vi.useRealTimers()
  })

  it('prevents edit when readOnly', () => {
    render(
      <MarkdownEditor
        initialContent="Content"
        versionId="v1"
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        readOnly={true}
      />
    )

    const textarea = screen.getByTestId('md-editor').querySelector('textarea')
    fireEvent.change(textarea!, { target: { value: 'Should not change' } })

    // Content should remain unchanged because onChange checks readOnly
    expect(textarea).toHaveValue('Content')
  })
})
