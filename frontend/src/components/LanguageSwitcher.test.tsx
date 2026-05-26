import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { LanguageSwitcher } from './LanguageSwitcher'

const mockSetLng = vi.fn()
const mockLng = 'en'

vi.mock('@/store/language.store', () => ({
  useLanguageStore: () => ({
    lng: mockLng,
    setLng: mockSetLng,
  }),
}))

vi.mock('lucide-react', () => ({
  Globe: () => <span data-testid="globe-icon">🌐</span>,
  Check: () => <span data-testid="check-icon">✓</span>,
}))

vi.mock('@/components/ui/button', () => ({
  Button: ({ children, variant, size, className, onClick }: any) => (
    <button data-variant={variant} data-size={size} className={className} onClick={onClick}>
      {children}
    </button>
  ),
}))

vi.mock('@/components/ui/dropdown-menu', () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children, asChild }: any) => children,
  DropdownMenuContent: ({ children, align }: any) => <div data-align={align}>{children}</div>,
  DropdownMenuItem: ({ children, className, onClick }: any) => (
    <button className={className} onClick={onClick}>{children}</button>
  ),
}))

describe('LanguageSwitcher', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders globe icon button', () => {
    render(<LanguageSwitcher />)
    expect(screen.getByTestId('globe-icon')).toBeInTheDocument()
  })

  it('renders all language options', () => {
    render(<LanguageSwitcher />)
    expect(screen.getByText('English')).toBeInTheDocument()
    expect(screen.getByText('简体中文')).toBeInTheDocument()
    expect(screen.getByText('繁體中文')).toBeInTheDocument()
  })

  it('shows check icon for current language', () => {
    render(<LanguageSwitcher />)
    const englishBtn = screen.getByText('English').closest('button')
    expect(englishBtn?.querySelector('[data-testid="check-icon"]')).toBeInTheDocument()
  })

  it('does not show check icon for non-current languages', () => {
    render(<LanguageSwitcher />)
    const chineseBtn = screen.getByText('简体中文').closest('button')
    expect(chineseBtn?.querySelector('[data-testid="check-icon"]')).not.toBeInTheDocument()
  })

  it('calls setLng when language clicked', () => {
    render(<LanguageSwitcher />)
    fireEvent.click(screen.getByText('简体中文'))
    expect(mockSetLng).toHaveBeenCalledWith('zh-CN')
  })

  it('calls setLng with correct code for each language', () => {
    render(<LanguageSwitcher />)
    fireEvent.click(screen.getByText('English'))
    expect(mockSetLng).toHaveBeenCalledWith('en')

    fireEvent.click(screen.getByText('繁體中文'))
    expect(mockSetLng).toHaveBeenCalledWith('zh-TW')
  })

  it('has correct button variant and size', () => {
    render(<LanguageSwitcher />)
    const btn = screen.getByTestId('globe-icon').closest('button')
    expect(btn).toHaveAttribute('data-variant', 'ghost')
    expect(btn).toHaveAttribute('data-size', 'icon')
  })

  it('renders with screen reader only text', () => {
    render(<LanguageSwitcher />)
    expect(screen.getByText('Switch language')).toBeInTheDocument()
  })

  it('has cursor-pointer class on menu items', () => {
    render(<LanguageSwitcher />)
    const items = screen.getAllByText(/English|简体中文|繁體中文/)
    items.forEach((item) => {
      expect(item.closest('button')).toHaveClass('cursor-pointer')
    })
  })

  it('renders dropdown content aligned to end', () => {
    render(<LanguageSwitcher />)
    expect(screen.getByText('English').closest('[data-align="end"]')).toBeInTheDocument()
  })
})
