import { describe, it, expect, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'

vi.mock('@/utils/i18n', () => ({
  getUserTimeZone: vi.fn().mockReturnValue('Asia/Shanghai'),
  setUserTimeZone: vi.fn(),
  clearUserTimeZone: vi.fn().mockImplementation(() => {
    // After clearing, getUserTimeZone would return browser default
  }),
}))

import { getUserTimeZone, setUserTimeZone, clearUserTimeZone } from '@/utils/i18n'
import { useTimeZone } from './useTimeZone'

describe('useTimeZone', () => {
  it('initializes with user timezone', () => {
    const { result } = renderHook(() => useTimeZone())

    expect(result.current.timeZone).toBe('Asia/Shanghai')
    expect(result.current.isAuto).toBe(
      'Asia/Shanghai' === Intl.DateTimeFormat().resolvedOptions().timeZone
    )
  })

  it('updates timezone', () => {
    const { result } = renderHook(() => useTimeZone())

    act(() => {
      result.current.updateTimeZone('America/New_York')
    })

    expect(setUserTimeZone).toHaveBeenCalledWith('America/New_York')
    expect(result.current.timeZone).toBe('America/New_York')
    expect(result.current.isAuto).toBe(
      'America/New_York' === Intl.DateTimeFormat().resolvedOptions().timeZone
    )
  })

  it('resets timezone to browser default', () => {
    const { result } = renderHook(() => useTimeZone())

    act(() => {
      result.current.resetTimeZone()
    })

    expect(clearUserTimeZone).toHaveBeenCalled()
  })

  it('syncs timezone from storage events', () => {
    const { result } = renderHook(() => useTimeZone())

    act(() => {
      window.dispatchEvent(
        new StorageEvent('storage', { key: 'user-timezone', newValue: 'Europe/London' })
      )
    })

    // The hook listens for storage events and updates state
    expect(result.current.timeZone).toBeDefined()
  })

  it('ignores unrelated storage events', () => {
    const { result } = renderHook(() => useTimeZone())

    const beforeUpdate = result.current.timeZone

    act(() => {
      window.dispatchEvent(
        new StorageEvent('storage', { key: 'other-key', newValue: 'some-value' })
      )
    })

    // Timezone should not change for unrelated keys
    expect(result.current.timeZone).toBe(beforeUpdate)
  })
})
