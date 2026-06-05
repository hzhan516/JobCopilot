import { describe, it, expect, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'

import { useAbortableRequest } from './useAbortableRequest'

describe('useAbortableRequest', () => {
  it('returns execute, abort, and getSignal functions', () => {
    const { result } = renderHook(() => useAbortableRequest())

    expect(typeof result.current.execute).toBe('function')
    expect(typeof result.current.abort).toBe('function')
    expect(typeof result.current.getSignal).toBe('function')
  })

  it('executes function with abort signal', async () => {
    const { result } = renderHook(() => useAbortableRequest())

    const fn = vi.fn(async (signal: AbortSignal) => {
      expect(signal).toBeInstanceOf(AbortSignal)
      return 'done'
    })

    const response = await act(async () => {
      return result.current.execute(fn)
    })

    expect(response).toBe('done')
    expect(fn).toHaveBeenCalledTimes(1)
  })

  it('cancels previous request on new execute', async () => {
    const { result } = renderHook(() => useAbortableRequest())

    let firstSignal: AbortSignal | null = null
    const slowFn = vi.fn(async (signal: AbortSignal) => {
      firstSignal = signal
      await new Promise((_, reject) => {
        signal.addEventListener('abort', () => reject(new Error('Aborted')))
      })
    })

    const fastFn = vi.fn(async () => 'fast')

    const slowPromise = result.current.execute(slowFn)

    // Immediately supersede with fast request
    const fastPromise = result.current.execute(fastFn)

    await expect(slowPromise).rejects.toThrow('Aborted')
    await expect(fastPromise).resolves.toBe('fast')
    expect(firstSignal?.aborted).toBe(true)
  })

  it('aborts manually with reason', () => {
    const { result } = renderHook(() => useAbortableRequest())

    let capturedSignal: AbortSignal | null = null
    const fn = vi.fn(async (signal: AbortSignal) => {
      capturedSignal = signal
      await new Promise((_, reject) => {
        signal.addEventListener('abort', () => reject(new Error('Aborted')))
      })
    })

    result.current.execute(fn).catch(() => undefined)

    expect(capturedSignal?.aborted).toBe(false)

    result.current.abort('Manual abort')

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('clears controller after successful completion', async () => {
    const { result } = renderHook(() => useAbortableRequest())

    await act(async () => {
      await result.current.execute(async () => 'done')
    })

    // getSignal should return undefined after successful completion
    expect(result.current.getSignal()).toBeUndefined()
  })

  it('aborts on component unmount', () => {
    let capturedSignal: AbortSignal | null = null
    const fn = vi.fn(async (signal: AbortSignal) => {
      capturedSignal = signal
      await new Promise(() => {}) // never resolves
    })

    const { result, unmount } = renderHook(() => useAbortableRequest())

    result.current.execute(fn)

    expect(capturedSignal?.aborted).toBe(false)

    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('returns current signal', () => {
    const { result } = renderHook(() => useAbortableRequest())

    expect(result.current.getSignal()).toBeUndefined()

    result.current.execute(async () => new Promise(() => {}))

    expect(result.current.getSignal()).toBeInstanceOf(AbortSignal)
  })
})
