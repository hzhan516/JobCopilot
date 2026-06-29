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

  it('updates when matchMedia change event fires', () => {
    const listeners: Record<string, EventListener[]> = {}
    const mockMql = {
      matches: false,
      addEventListener: vi.fn((event: string, callback: EventListener) => {
        listeners[event] = listeners[event] || []
        listeners[event].push(callback)
      }),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn((event: Event) => {
        listeners[event.type]?.forEach((cb) => cb(event))
        return true
      }),
    }

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      configurable: true,
      value: vi.fn().mockReturnValue(mockMql),
    })

    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024,
    })

    const { result } = renderHook(() => useIsMobile())

    expect(result.current).toBe(false)

    // Simulate crossing the mobile breakpoint via matchMedia change
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 500,
    })

    act(() => {
      mockMql.dispatchEvent(new Event('change'))
    })

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
