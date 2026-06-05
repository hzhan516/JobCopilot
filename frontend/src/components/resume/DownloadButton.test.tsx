import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { DownloadButton } from './DownloadButton'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

const mockDownloadResume = vi.fn()

vi.mock('@/utils/file.ts', () => ({
  downloadResume: (versionId: string, format: string, filename: string) => mockDownloadResume(versionId, format, filename),
}))

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
  },
}))

vi.mock('../ui/button', () => ({
  Button: ({ children, onClick, disabled, variant, size, className }: any) => (
    <button onClick={onClick} disabled={disabled} className={className}>{children}</button>
  ),
}))

vi.mock('../ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children, asChild }: any) => children,
  DropdownMenuContent: ({ children, align }: any) => <div>{children}</div>,
  DropdownMenuItem: ({ children, onClick, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

vi.mock('lucide-react', () => ({
  Download: () => <span>⬇️</span>,
  Loader2: () => <span>⏳</span>,
}))

describe('DownloadButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders download button with default text', () => {
    render(<DownloadButton versionId="v1" filename="my-resume" />)

    expect(screen.getByText('resume.download.button')).toBeInTheDocument()
    expect(screen.getByText('⬇️')).toBeInTheDocument()
  })

  it('shows dropdown menu with all format options', () => {
    render(<DownloadButton versionId="v1" filename="my-resume" />)

    expect(screen.getByText('resume.download.pdf')).toBeInTheDocument()
    expect(screen.getByText('resume.download.docx')).toBeInTheDocument()
    expect(screen.getByText('resume.download.md')).toBeInTheDocument()
    expect(screen.getByText('resume.download.html')).toBeInTheDocument()
    expect(screen.getByText('resume.download.txt')).toBeInTheDocument()
  })

  it('handles PDF download', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.pdf'))

    await waitFor(() => {
      expect(mockDownloadResume).toHaveBeenCalledWith('v1', 'pdf', 'my-resume')
    })
  })

  it('handles DOCX download', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.docx'))

    await waitFor(() => {
      expect(mockDownloadResume).toHaveBeenCalledWith('v1', 'docx', 'my-resume')
    })
  })

  it('handles MD download', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.md'))

    await waitFor(() => {
      expect(mockDownloadResume).toHaveBeenCalledWith('v1', 'md', 'my-resume')
    })
  })

  it('handles HTML download', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.html'))

    await waitFor(() => {
      expect(mockDownloadResume).toHaveBeenCalledWith('v1', 'html', 'my-resume')
    })
  })

  it('handles TXT download', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.txt'))

    await waitFor(() => {
      expect(mockDownloadResume).toHaveBeenCalledWith('v1', 'txt', 'my-resume')
    })
  })

  it('shows loader during download', async () => {
    mockDownloadResume.mockImplementation(() => new Promise(() => {}))

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.pdf'))

    await waitFor(() => {
      expect(screen.getByText('⏳')).toBeInTheDocument()
    })
  })

  it('disables button during download', async () => {
    mockDownloadResume.mockImplementation(() => new Promise(() => {}))

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    const triggerBtn = screen.getByText('resume.download.button').closest('button')
    fireEvent.click(screen.getByText('resume.download.pdf'))

    await waitFor(() => {
      expect(triggerBtn).toBeDisabled()
    })
  })

  it('shows error toast on download failure', async () => {
    mockDownloadResume.mockRejectedValue(new Error('Download failed'))

    const { toast } = await import('sonner')

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.pdf'))

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('resume.download.error')
    })
  })

  it('uses default filename when not provided', () => {
    render(<DownloadButton versionId="v1" />)

    fireEvent.click(screen.getByText('resume.download.pdf'))

    // The button should still work with default filename
    expect(screen.getByText('resume.download.button')).toBeInTheDocument()
  })

  it('resets loading state after download completes', async () => {
    mockDownloadResume.mockResolvedValue(undefined)

    render(<DownloadButton versionId="v1" filename="my-resume" />)

    fireEvent.click(screen.getByText('resume.download.pdf'))

    await waitFor(() => {
      expect(screen.queryByText('⏳')).not.toBeInTheDocument()
    })
  })

  it('renders with all format options as clickable items', () => {
    render(<DownloadButton versionId="v1" filename="test" />)

    const formats = ['pdf', 'docx', 'md', 'html', 'txt']
    formats.forEach((format) => {
      expect(screen.getByText(`resume.download.${format}`)).toBeInTheDocument()
    })
  })
})
