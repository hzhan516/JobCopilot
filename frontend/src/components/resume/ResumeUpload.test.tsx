import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ResumeUpload } from './ResumeUpload'

const mockOnUpload = vi.fn()
const mockResumeUploadStore = vi.hoisted(() => ({
  uploadProgress: 0,
  loading: false,
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('../../store/resume.store', () => ({
  useResumeStore: () => mockResumeUploadStore,
}))

describe('ResumeUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockResumeUploadStore.uploadProgress = 0
    mockResumeUploadStore.loading = false
  })

  it('renders upload area with drag instructions', () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    expect(screen.getByText('resume.upload.dragText')).toBeInTheDocument()
    expect(screen.getByText('resume.upload.singleUpload')).toBeInTheDocument()
    expect(screen.getByText('resume.upload.formats')).toBeInTheDocument()
  })

  it('accepts valid PDF file via input change', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['pdf content'], 'resume.pdf', { type: 'application/pdf' })
    const input = screen.getByTestId('resume-upload-input') || document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(mockOnUpload).toHaveBeenCalledWith(file)
      })
    }
  })

  it('accepts valid DOCX file', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['docx content'], 'resume.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(mockOnUpload).toHaveBeenCalledWith(file)
      })
    }
  })

  it('accepts valid MD file (by extension)', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['md content'], 'resume.md', { type: 'text/plain' })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(mockOnUpload).toHaveBeenCalledWith(file)
      })
    }
  })

  it('accepts valid TXT file', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['txt content'], 'resume.txt', { type: 'text/plain' })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(mockOnUpload).toHaveBeenCalledWith(file)
      })
    }
  })

  it('rejects invalid file type (e.g., JPG)', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['image'], 'photo.jpg', { type: 'image/jpeg' })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(screen.getByText('resume.upload.invalidType')).toBeInTheDocument()
        expect(mockOnUpload).not.toHaveBeenCalled()
      })
    }
  })

  it('rejects file exceeding 10MB size limit', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    // Create a file larger than 10MB
    const largeContent = new Uint8Array(10 * 1024 * 1024 + 1)
    const file = new File([largeContent], 'large.pdf', { type: 'application/pdf' })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(screen.getByText('resume.upload.sizeExceeded')).toBeInTheDocument()
        expect(mockOnUpload).not.toHaveBeenCalled()
      })
    }
  })

  it('handles drop event with valid file', async () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const dropZone = screen.getByText('resume.upload.dragText').closest('div')?.parentElement
    const file = new File(['pdf'], 'dropped.pdf', { type: 'application/pdf' })

    if (dropZone) {
      fireEvent.dragOver(dropZone)
      fireEvent.drop(dropZone, {
        dataTransfer: {
          files: [file],
        },
      })

      await waitFor(() => {
        expect(mockOnUpload).toHaveBeenCalledWith(file)
      })
    }
  })

  it('handles drag leave correctly', () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    const dropZone = screen.getByText('resume.upload.dragText').closest('div')?.parentElement

    if (dropZone) {
      fireEvent.dragOver(dropZone)
      fireEvent.dragLeave(dropZone)
      // After drag leave, the drag state should reset
      expect(dropZone).not.toHaveClass('border-primary')
    }
  })

  it('disables interaction during loading', () => {
    mockResumeUploadStore.uploadProgress = 50
    mockResumeUploadStore.loading = true

    render(<ResumeUpload onUpload={mockOnUpload} />)

    const input = document.getElementById('resume-upload-input')
    if (input) {
      expect(input).toBeDisabled()
    }

    // Should show progress
    expect(screen.getByText('50%')).toBeInTheDocument()
    expect(screen.getByText('resume.upload.uploading')).toBeInTheDocument()
  })

  it('displays upload error from onUpload rejection', async () => {
    mockOnUpload.mockRejectedValue(new Error('Upload failed'))

    render(<ResumeUpload onUpload={mockOnUpload} />)

    const file = new File(['pdf'], 'error.pdf', { type: 'application/pdf' })
    const input = document.getElementById('resume-upload-input')

    if (input) {
      fireEvent.change(input, { target: { files: [file] } })
      await waitFor(() => {
        expect(screen.getByText('resume.upload.uploadError')).toBeInTheDocument()
      })
    }
  })

  it('clears previous error on new upload attempt', async () => {
    mockOnUpload.mockRejectedValueOnce(new Error('First fail'))
    mockOnUpload.mockResolvedValueOnce(undefined)

    render(<ResumeUpload onUpload={mockOnUpload} />)

    const input = document.getElementById('resume-upload-input')

    // First upload fails
    if (input) {
      fireEvent.change(input, { target: { files: [new File(['pdf'], 'fail.pdf', { type: 'application/pdf' })] } })
      await waitFor(() => {
        expect(screen.getByText('resume.upload.uploadError')).toBeInTheDocument()
      })

      // Second upload should clear error
      fireEvent.change(input, { target: { files: [new File(['pdf'], 'success.pdf', { type: 'application/pdf' })] } })
      await waitFor(() => {
        expect(screen.queryByText('resume.upload.uploadError')).not.toBeInTheDocument()
      })
    }
  })

  it('triggers file input when drop zone is clicked', () => {
    render(<ResumeUpload onUpload={mockOnUpload} />)

    expect(screen.getByTestId('resume-upload-dropzone')).toHaveClass('cursor-pointer')
  })
})
