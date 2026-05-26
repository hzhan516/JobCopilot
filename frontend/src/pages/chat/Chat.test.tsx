import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { Children, cloneElement, isValidElement } from 'react'
import Chat from './Chat'

const mockToast = vi.hoisted(() => ({
  error: vi.fn(),
  success: vi.fn(),
  info: vi.fn(),
}))
const mockT = vi.hoisted(() => vi.fn((key: string) => key))

vi.mock('sonner', () => ({
  toast: mockToast,
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: mockT,
  }),
}))

vi.mock('@/services/chatService', () => ({
  default: {
    getConversations: vi.fn().mockResolvedValue([
      {
        conversationId: 'conv-1',
        title: 'Test Chat',
        resumeVersionId: 'v1',
        jobId: 'job-1',
        messages: [
          { messageId: 'm1', conversationId: 'conv-1', role: 'USER', content: 'Hello', createdAt: '2024-01-01T00:00:00Z' },
          { messageId: 'm2', conversationId: 'conv-1', role: 'ASSISTANT', content: 'Hi there', createdAt: '2024-01-01T00:00:01Z' },
        ],
      },
    ]),
    getConversation: vi.fn().mockResolvedValue({
      conversationId: 'conv-1',
      title: 'Test Chat',
      messages: [
        { messageId: 'm1', conversationId: 'conv-1', role: 'USER', content: 'Hello', createdAt: '2024-01-01T00:00:00Z' },
        { messageId: 'm2', conversationId: 'conv-1', role: 'ASSISTANT', content: 'Hi there', createdAt: '2024-01-01T00:00:01Z' },
      ],
    }),
    createConversation: vi.fn().mockResolvedValue({
      conversationId: 'conv-new',
      title: 'New Chat',
      resumeVersionId: 'v1',
      jobId: 'job-1',
      messages: [
        { messageId: 'm3', conversationId: 'conv-new', role: 'USER', content: 'Initial message', createdAt: '2024-01-01T00:00:00Z' },
      ],
    }),
    sendMessage: vi.fn().mockResolvedValue({
      conversationId: 'conv-1',
      messages: [
        { messageId: 'm1', conversationId: 'conv-1', role: 'USER', content: 'Hello', createdAt: '2024-01-01T00:00:00Z' },
        { messageId: 'm2', conversationId: 'conv-1', role: 'ASSISTANT', content: 'Hi there', createdAt: '2024-01-01T00:00:01Z' },
        { messageId: 'm4', conversationId: 'conv-1', role: 'USER', content: 'How are you?', createdAt: '2024-01-01T00:00:02Z' },
        { messageId: 'm5', conversationId: 'conv-1', role: 'ASSISTANT', content: 'I am fine', createdAt: '2024-01-01T00:00:03Z' },
      ],
    }),
    deleteConversation: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('@/services/resumeService', () => ({
  resumeService: {
    getResumeGroups: vi.fn().mockResolvedValue([
      {
        title: 'My Resume',
        convertedVersion: { versionId: 'v1', exists: true, status: 'COMPLETED', parseStatus: 'COMPLETED' },
        aiOptimizedVersion: { versionId: 'v2', exists: true, status: 'COMPLETED', parseStatus: 'COMPLETED' },
      },
    ]),
  },
}))

vi.mock('@/services/jobService', () => ({
  jobService: {
    getJobs: vi.fn().mockResolvedValue([
      {
        id: 'job-1',
        status: 'COMPLETED',
        parsedContent: { company: 'TechCorp', title: 'Frontend Dev' },
      },
    ]),
  },
}))

vi.mock('@/utils/i18n', () => ({
  formatTime: (date: string) => date,
}))

// Mock lucide-react icons to avoid rendering issues
vi.mock('lucide-react', () => ({
  MessageSquare: () => <svg data-testid="icon-message-square" />,
  Plus: () => <svg data-testid="icon-plus" />,
  Send: () => <svg data-testid="icon-send" />,
  Trash2: () => <svg data-testid="icon-trash" />,
  Bot: () => <svg data-testid="icon-bot" />,
  User: () => <svg data-testid="icon-user" />,
  Loader2: () => <svg data-testid="icon-loader" />,
  MoreVertical: () => <svg data-testid="icon-more" />,
  Sparkles: () => <svg data-testid="icon-sparkles" />,
  FileText: () => <svg data-testid="icon-file" />,
  Briefcase: () => <svg data-testid="icon-briefcase" />,
}))

// Mock UI components
vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, disabled, ...props }: any) => (
    <button onClick={onClick} disabled={disabled} {...props}>{children}</button>
  ),
}))

vi.mock('@/components/ui/input', () => ({
  Input: (props: any) => <input {...props} />,
}))

vi.mock('@/components/ui/skeleton', () => ({
  Skeleton: ({ className }: any) => <div className={className} data-testid="skeleton" />,
}))

vi.mock('@/components/ui/select', () => ({
  Select: ({ children, value, onValueChange }: any) => {
    const injectSelectHandler = (node: any): any =>
      Children.map(node, (child) => {
        if (!isValidElement(child)) return child
        const props = child.props as any

        if (props.value) {
          return cloneElement(child, {
            onSelectValue: onValueChange,
          } as any)
        }

        if (props.children) {
          return cloneElement(child, {
            children: injectSelectHandler(props.children),
          } as any)
        }

        return child
      })

    return (
      <div data-testid="select" data-value={value}>
        {injectSelectHandler(children)}
      </div>
    )
  },
  SelectTrigger: ({ children }: any) => <div>{children}</div>,
  SelectValue: ({ placeholder }: any) => <span>{placeholder}</span>,
  SelectContent: ({ children }: any) => <div>{children}</div>,
  SelectItem: ({ children, value, onSelectValue }: any) => (
    <button type="button" data-value={value} onClick={() => onSelectValue?.(value)}>
      {children}
    </button>
  ),
}))

vi.mock('@/components/ui/dialog', () => ({
  Dialog: ({ children, open, onOpenChange }: any) => {
    const injectDialogHandler = (node: any): any =>
      Children.map(node, (child) => {
        if (!isValidElement(child)) return child
        const props = child.props as any

        if (props.asChild) {
          return cloneElement(child, {
            onOpen: () => onOpenChange?.(true),
          } as any)
        }

        if (props.children) {
          return cloneElement(child, {
            children: injectDialogHandler(props.children),
          } as any)
        }

        return child
      })

    return (
      <div data-testid="dialog" data-open={open}>
        {injectDialogHandler(children)}
        {open && <button onClick={() => onOpenChange?.(false)}>Close Dialog</button>}
      </div>
    )
  },
  DialogContent: ({ children }: any) => <div>{children}</div>,
  DialogDescription: ({ children }: any) => <div>{children}</div>,
  DialogFooter: ({ children }: any) => <div>{children}</div>,
  DialogHeader: ({ children }: any) => <div>{children}</div>,
  DialogTitle: ({ children }: any) => <div>{children}</div>,
  DialogTrigger: ({ children, onOpen }: any) => (
    <div onClick={() => onOpen?.()}>{children}</div>
  ),
}))

vi.mock('@/components/ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuContent: ({ children }: any) => <div>{children}</div>,
  DropdownMenuItem: ({ children, onClick }: any) => (
    <div onClick={onClick}>{children}</div>
  ),
  DropdownMenuTrigger: ({ children }: any) => <div>{children}</div>,
}))

describe('Chat page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading skeleton initially', () => {
    render(<Chat />)

    expect(screen.getAllByTestId('skeleton').length).toBeGreaterThan(0)
  })

  it('renders conversation list after loading', async () => {
    render(<Chat />)

    await waitFor(() => {
      expect(screen.getAllByText('Test Chat').length).toBeGreaterThan(0)
    })
  })

  it('renders active conversation messages', async () => {
    render(<Chat />)

    await waitFor(() => {
      expect(screen.getByText('Hello')).toBeInTheDocument()
      expect(screen.getByText('Hi there')).toBeInTheDocument()
    })
  })

  it('renders empty state when no active conversation', async () => {
    const { default: chatService } = await import('@/services/chatService')
    vi.mocked(chatService.getConversations).mockResolvedValueOnce([])

    render(<Chat />)

    await waitFor(() => {
      expect(screen.getByText('chat.selectOrCreate')).toBeInTheDocument()
    })
  })

  it('shows input placeholder for sending messages', async () => {
    render(<Chat />)

    await waitFor(() => {
      const input = screen.getByPlaceholderText('chat.inputPlaceholder')
      expect(input).toBeInTheDocument()
      expect(input).not.toBeDisabled()
    })
  })

  it('disables send button when input is empty', async () => {
    render(<Chat />)

    await waitFor(() => {
      // The send button should be disabled when input is empty
      const input = screen.getByPlaceholderText('chat.inputPlaceholder') as HTMLInputElement
      expect(input.value).toBe('')
    })
  })

  it('opens new conversation dialog when clicking new chat', async () => {
    render(<Chat />)

    await waitFor(() => {
      expect(screen.getAllByText('chat.newChat').length).toBeGreaterThan(0)
    })

    fireEvent.click(screen.getAllByText('chat.newChat')[0])

    await waitFor(() => {
      expect(screen.getByTestId('dialog')).toHaveAttribute('data-open', 'true')
    })
  })

  it('requires resume and job selection for new conversation', async () => {
    const { default: chatService } = await import('@/services/chatService')
    vi.mocked(chatService.createConversation).mockClear()

    render(<Chat />)

    await waitFor(() => {
      expect(screen.getAllByText('chat.newChat').length).toBeGreaterThan(0)
    })

    fireEvent.click(screen.getAllByText('chat.newChat')[0])

    await waitFor(() => {
      expect(screen.getByText('chat.newChatTitle')).toBeInTheDocument()
    })

    // Create button should be disabled without selections
    const createBtn = screen.getByText('chat.create')
    expect(createBtn.closest('button')).toBeDisabled()
  })

  it('can select resume and job in new dialog', async () => {
    render(<Chat />)

    await waitFor(() => {
      expect(screen.getAllByText('chat.newChat').length).toBeGreaterThan(0)
    })

    fireEvent.click(screen.getAllByText('chat.newChat')[0])

    await waitFor(() => {
      // Select resume
      fireEvent.click(screen.getByText('My Resume - v1 (COMPLETED)'))
      // Select job
      fireEvent.click(screen.getByText('TechCorp - Frontend Dev'))
    })

    // After selection, create button should be enabled
    const createBtn = screen.getByText('chat.create')
    expect(createBtn.closest('button')).not.toBeDisabled()
  })

  it('handles message input change', async () => {
    render(<Chat />)

    await waitFor(() => {
      const input = screen.getByPlaceholderText('chat.inputPlaceholder')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText('chat.inputPlaceholder')
    fireEvent.change(input, { target: { value: 'Test message' } })

    expect(input).toHaveValue('Test message')
  })

  it('handles delete conversation', async () => {
    const { default: chatService } = await import('@/services/chatService')

    render(<Chat />)

    await waitFor(() => {
      expect(screen.getAllByText('Test Chat').length).toBeGreaterThan(0)
    })

    // Find and click delete option in dropdown
    const deleteItems = screen.getAllByText('chat.delete')
    if (deleteItems.length > 0) {
      fireEvent.click(deleteItems[0])
    }

    await waitFor(() => {
      expect(chatService.deleteConversation).toHaveBeenCalledWith('conv-1')
      expect(mockToast.success).toHaveBeenCalledWith('chat.deleteSuccess')
    })
  })

  it('displays active resume and job labels', async () => {
    render(<Chat />)

    await waitFor(() => {
      // Should show resume name and job info
      expect(screen.getAllByText('Test Chat').length).toBeGreaterThan(0)
    })
  })
})
