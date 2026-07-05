import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Chat from './Chat'

// copilot.store 的 open 函数 mock
const mockOpen = vi.fn()

vi.mock('@/store/copilot.store', () => ({
  useCopilotStore: () => ({
    open: mockOpen,
    isOpen: false,
    close: vi.fn(),
  }),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

// Mock lucide-react icons
vi.mock('lucide-react', () => ({
  MessageSquare: () => <svg data-testid="icon-message-square" />,
  ArrowRight: () => <svg data-testid="icon-arrow-right" />,
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, onClick, variant, size, className }: any) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
}))

describe('Chat page (drawer redirect)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('auto-opens copilot drawer on mount', () => {
    render(<Chat />)
    expect(mockOpen).toHaveBeenCalledWith({ type: 'general' })
  })

  it('renders guide placeholder with chat title', () => {
    render(<Chat />)
    expect(screen.getByText('layout.nav.chat')).toBeInTheDocument()
  })

  it('renders drawer redirect message', () => {
    render(<Chat />)
    expect(screen.getByText('chat.drawerRedirect')).toBeInTheDocument()
  })

  it('renders open copilot button', () => {
    render(<Chat />)
    expect(screen.getByText('layout.sidebar.copilot.openCopilot')).toBeInTheDocument()
  })

  it('calls open when clicking the button', () => {
    render(<Chat />)
    // open already called once on mount
    expect(mockOpen).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByText('layout.sidebar.copilot.openCopilot'))
    expect(mockOpen).toHaveBeenCalledTimes(2)
    expect(mockOpen).toHaveBeenLastCalledWith({ type: 'general' })
  })
})
