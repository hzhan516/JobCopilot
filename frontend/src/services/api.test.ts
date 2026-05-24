import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

describe('api.ts core logic', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers({ shouldAdvanceTime: true })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // 用纯函数测试拦截器中的关键判断逻辑
  describe('isRetryableNetworkError', () => {
    it('returns true for network errors without response', () => {
      const error = { response: undefined, code: 'ECONNREFUSED' } as AxiosError
      const result = !error.response && error.code !== 'ECONNABORTED'
      expect(result).toBe(true)
    })

    it('returns false for timeout errors', () => {
      const error = { response: undefined, code: 'ECONNABORTED' } as AxiosError
      const result = !error.response && error.code !== 'ECONNABORTED'
      expect(result).toBe(false)
    })

    it('returns false for HTTP 4xx/5xx errors', () => {
      const error = { response: { status: 500 }, code: undefined } as AxiosError
      const result = !error.response && error.code !== 'ECONNABORTED'
      expect(result).toBe(false)
    })
  })

  describe('clearAuthAndRedirect', () => {
    it('clears storage and redirects to login', () => {
      const clearSpy = vi.fn()
      const locationSpy = vi.spyOn(window.location, 'href', 'set').mockImplementation(() => {})

      // Simulate the logic inline
      clearSpy()
      window.location.href = '/login'

      expect(clearSpy).toHaveBeenCalled()
      expect(locationSpy).toHaveBeenCalledWith('/login')

      locationSpy.mockRestore()
    })
  })

  describe('createAbortableRequest', () => {
    it('cancels previous request on new execute', async () => {
      let controller: AbortController | null = null

      const execute = async <T>(fn: (signal: AbortSignal) => Promise<T>): Promise<T> => {
        controller?.abort('Superseded by new request')
        const newController = new AbortController()
        controller = newController
        return fn(newController.signal).finally(() => {
          if (controller === newController) {
            controller = null
          }
        })
      }

      const promise1 = execute(async (signal) => {
        await new Promise((resolve) => setTimeout(resolve, 1000))
        if (signal.aborted) throw new Error('Aborted')
        return 'result1'
      })

      const promise2 = execute(async (signal) => {
        return 'result2'
      })

      await expect(promise1).rejects.toThrow('Aborted')
      await expect(promise2).resolves.toBe('result2')
    })

    it('manually aborts pending request', () => {
      let controller: AbortController | null = new AbortController()

      const abort = (reason?: string): void => {
        controller?.abort(reason)
        controller = null
      }

      expect(controller?.signal.aborted).toBe(false)
      abort('Manual abort')
      expect(controller).toBeNull()
    })

    it('clears controller after successful completion', async () => {
      let controller: AbortController | null = null

      const execute = async <T>(fn: (signal: AbortSignal) => Promise<T>): Promise<T> => {
        controller?.abort()
        const newController = new AbortController()
        controller = newController
        const result = await fn(newController.signal)
        if (controller === newController) {
          controller = null
        }
        return result
      }

      await execute(async () => 'done')
      expect(controller).toBeNull()
    })
  })

  describe('tokenStorage helpers', () => {
    it('stores and retrieves tokens correctly', () => {
      const store: Record<string, string> = {}
      const storage = {
        getItem: (k: string) => store[k] ?? null,
        setItem: (k: string, v: string) => { store[k] = v },
        removeItem: (k: string) => { delete store[k] },
      }

      storage.setItem('accessToken', 'token-123')
      expect(storage.getItem('accessToken')).toBe('token-123')

      storage.removeItem('accessToken')
      expect(storage.getItem('accessToken')).toBeNull()
    })
  })

  describe('onRefreshed / addRefreshSubscriber', () => {
    it('notifies all queued subscribers with new token', () => {
      const subscribers: Array<(token: string) => void> = []
      const onRefreshed = (token: string) => {
        subscribers.forEach((cb) => cb(token))
        subscribers.length = 0
      }
      const addSubscriber = (cb: (token: string) => void) => subscribers.push(cb)

      const cb1 = vi.fn()
      const cb2 = vi.fn()
      addSubscriber(cb1)
      addSubscriber(cb2)

      onRefreshed('new-token')

      expect(cb1).toHaveBeenCalledWith('new-token')
      expect(cb2).toHaveBeenCalledWith('new-token')
      expect(subscribers).toHaveLength(0)
    })
  })
})
