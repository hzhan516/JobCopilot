import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ParseStatusBadge } from './ParseStatusBadge'
import type { ResumeVersion } from '@/types/resume'

// Isolate component from i18n dependencies for deterministic assertions
// 隔离组件与 i18n 依赖，确保断言结果确定可控
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

describe('ParseStatusBadge', () => {
  it('renders PENDING status', () => {
    render(<ParseStatusBadge status="PENDING" />)
    expect(screen.getByText('resume.parseStatus.pending')).toBeInTheDocument()
  })

  it('renders PARSING status with spinner', () => {
    render(<ParseStatusBadge status="PARSING" />)
    expect(screen.getByText('resume.parseStatus.parsing')).toBeInTheDocument()
  })

  it('renders COMPLETED status', () => {
    render(<ParseStatusBadge status="COMPLETED" />)
    expect(screen.getByText('resume.parseStatus.completed')).toBeInTheDocument()
  })

  it('renders FAILED status', () => {
    render(<ParseStatusBadge status="FAILED" />)
    expect(screen.getByText('resume.parseStatus.failed')).toBeInTheDocument()
  })

  it('returns null for NOT_APPLICABLE', () => {
    const { container } = render(<ParseStatusBadge status="NOT_APPLICABLE" />)
    expect(container.firstChild).toBeNull()
  })

  it('returns null for unknown status', () => {
    const { container } = render(<ParseStatusBadge status={'UNKNOWN' as ResumeVersion['parseStatus']} />)
    expect(container.firstChild).toBeNull()
  })
})
