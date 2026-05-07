import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'

const mockChangeLanguage = vi.fn()

vi.mock('@/i18n', () => ({
  default: {
    language: 'en',
    changeLanguage: vi.fn(),
  },
}))

// Need to access the mock after hoisting; re-assign via import
import mockI18n from '@/i18n'

import { useLanguageStore } from './language.store'

describe('useLanguageStore', () => {
  beforeEach(() => {
    mockChangeLanguage.mockClear()
    // Re-assign the mock function on the imported mock object
    ;(mockI18n.changeLanguage as ReturnType<typeof vi.fn>) = mockChangeLanguage
    useLanguageStore.setState({ lng: 'en' })
  })

  it('initializes with default language', () => {
    const { result } = renderHook(() => useLanguageStore())
    expect(result.current.lng).toBe('en')
  })

  it('changes language and calls i18n.changeLanguage', () => {
    const { result } = renderHook(() => useLanguageStore())

    act(() => {
      result.current.setLng('zh-CN')
    })

    expect(mockChangeLanguage).toHaveBeenCalledWith('zh-CN')
    expect(result.current.lng).toBe('zh-CN')
  })

  it('can switch language multiple times', () => {
    const { result } = renderHook(() => useLanguageStore())

    act(() => {
      result.current.setLng('zh-TW')
    })
    expect(result.current.lng).toBe('zh-TW')

    act(() => {
      result.current.setLng('en')
    })
    expect(result.current.lng).toBe('en')
    expect(mockChangeLanguage).toHaveBeenCalledTimes(2)
  })
})
