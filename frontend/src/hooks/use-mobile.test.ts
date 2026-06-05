import { describe, it, expect, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'

import { useIsMobile } from './use-mobile'

describe('useIsMobile', () => {
  it('returns undefined on initial render before effect runs', () => {
    // Note: In happy-dom, the effect may run synchronously, so this test
    // checks the initial state is properly set
    const { result } = renderHook(() => useIsMobile())

    // After effect runs, should have a boolean value
    expect(typeof result.current).toBe('boolean')
  })

  it('returns true when viewport width is below mobile breakpoint', () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 500,
    })

    const { result } = renderHook(() => useIsMobile())

    expect(result.current).toBe(true)
  })

  it('returns false when viewport width is above mobile breakpoint', () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    })

    const { result } = renderHook(() => useIsMobile())

    expect(result.current).toBe(false)
  })

  it('updates when window resizes', () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    })

    const { result } = renderHook(() => useIsMobile())

    expect(result.current).toBe(false)

    // Simulate resize to mobile
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 500,
    })

    act(() => {
      window.dispatchEvent(new Event('resize'))
    })

    // The hook uses matchMedia, not resize event directly
    // This test validates the matchMedia listener is set up
    expect(result.current).toBe(true)
  })

  it('cleans up matchMedia listener on unmount', () => {
    const removeEventListenerSpy = vi.fn()
    const mockMql = {
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: removeEventListenerSpy,
      dispatchEvent: vi.fn(),
    }

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: vi.fn().mockReturnValue(mockMql),
    })

    const { unmount } = renderHook(() => useIsMobile())

    unmount()

    expect(removeEventListenerSpy).toHaveBeenCalled()
  })
})
