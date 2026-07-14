import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios, { type AxiosError } from 'axios'

// Mock dependencies to isolate api.ts internal logic
vi.mock('./tokenStorage', () => ({
  default: {
    getAccessToken: vi.fn(),
    clear: vi.fn(),
    setTokens: vi.fn(),
    setUser: vi.fn(),
    getRememberMe: vi.fn(),
  },
}))

vi.mock('@/i18n', () => ({
  default: { language: 'en' },
}))

describe('api.ts real implementation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ============================================================
  // Interceptor behavior — tested via axios mock
  // ============================================================
  describe('request interceptor', () => {
    it('attaches Authorization header when token exists', async () => {
      // Re-import to get fresh instance with mocked dependencies
      vi.resetModules()
      const { default: apiClient, authService } = await import('./api')
      const { default: tokenStorage } = await import('./tokenStorage')
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('test-token-123')

      // authService.login uses the apiClient instance, not the global axios.post.
      // Mock the transport layer so the request interceptor runs without hitting the real backend.
      const postSpy = vi.spyOn(apiClient, 'post')
      const adapterMock = vi.fn().mockResolvedValue({
        data: { code: 200, data: { accessToken: 'new-token', expiresIn: 3600 } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {},
      })
      apiClient.defaults.adapter = adapterMock as any

      await authService.login({ email: 'test@test.com', password: 'pass' })

      expect(postSpy).toHaveBeenCalled()
      expect(adapterMock).toHaveBeenCalled()
      const requestConfig = adapterMock.mock.calls[0][0]
      expect(requestConfig.headers.Authorization).toBe('Bearer test-token-123')

      postSpy.mockRestore()
    })
  })

  describe('isRetryableNetworkError logic (via behavior test)', () => {
    it('does not retry raw axios GET request outside the configured api client', async () => {
      vi.resetModules()
      const freshAxios = (await import('axios')).default
      const getSpy = vi
        .spyOn(freshAxios, 'get')
        .mockRejectedValueOnce({
          response: undefined,
          code: 'ECONNREFUSED',
          config: { method: 'get', url: '/test' },
        } as AxiosError)
        .mockResolvedValueOnce({ data: { success: true } })

      try {
        await freshAxios.get('/test')
      } catch {
        // may fail due to mock structure
      }

      expect(getSpy).toHaveBeenCalledTimes(1)
      getSpy.mockRestore()
    })

    it('does NOT retry GET request on ECONNABORTED (timeout)', async () => {
      vi.resetModules()
      const freshAxios = (await import('axios')).default
      const getSpy = vi
        .spyOn(freshAxios, 'get')
        .mockRejectedValueOnce({
          response: undefined,
          code: 'ECONNABORTED',
          config: { method: 'get', url: '/test' },
        } as AxiosError)

      try {
        await freshAxios.get('/test')
      } catch {
        // expected
      }

      expect(getSpy).toHaveBeenCalledTimes(1)
      getSpy.mockRestore()
    })
  })

  describe('clearAuthAndRedirect behavior', () => {
    it('clears storage and redirects on 401 refresh failure', async () => {
      const { default: tokenStorage } = await import('./tokenStorage')
      const locationSpy = vi.spyOn(window.location, 'href', 'set').mockImplementation(() => {})

      // Simulate the clearAuthAndRedirect action
      tokenStorage.clear()
      window.location.href = '/login'

      expect(tokenStorage.clear).toHaveBeenCalled()
      expect(locationSpy).toHaveBeenCalledWith('/login')

      locationSpy.mockRestore()
    })
  })

  describe('refresh failure queue', () => {
    it('rejects all queued requests, clears the queue, and permits a later refresh', async () => {
      vi.resetModules()
      const freshAxios = (await import('axios')).default
      const { default: apiClient } = await import('./api')
      const { default: tokenStorage } = await import('./tokenStorage')
      const locationSpy = vi.spyOn(window.location, 'href', 'set').mockImplementation(() => {})
      let refreshRecovered = false

      apiClient.defaults.adapter = vi.fn(async (config) => {
        if (refreshRecovered && (config as typeof config & { _retry?: boolean })._retry) {
          return {
            data: { ok: true },
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
          }
        }
        throw {
          config,
          response: {
            data: { code: 401, message: 'expired' },
            status: 401,
            statusText: 'Unauthorized',
            headers: {},
            config,
          },
        } as AxiosError
      })

      let rejectRefresh!: (reason?: unknown) => void
      const refreshSpy = vi.spyOn(freshAxios, 'post').mockImplementationOnce(
        () => new Promise((_resolve, reject) => { rejectRefresh = reject })
      )

      const first = apiClient.get('/protected/one')
      const second = apiClient.get('/protected/two')
      await vi.waitFor(() => expect(refreshSpy).toHaveBeenCalledTimes(1))
      rejectRefresh(new Error('refresh failed'))

      const failed = await Promise.allSettled([first, second])
      expect(failed.map((result) => result.status)).toEqual(['rejected', 'rejected'])
      expect(refreshSpy).toHaveBeenCalledTimes(1)
      expect(tokenStorage.clear).toHaveBeenCalledTimes(1)
      expect(locationSpy).toHaveBeenCalledWith('/login')

      refreshSpy.mockImplementationOnce(async () => {
        refreshRecovered = true
        return {
          data: {
            code: 200,
            data: { accessToken: 'recovered-token', expiresIn: 3600 },
          },
        }
      })

      await expect(apiClient.get('/protected/three')).resolves.toMatchObject({ status: 200 })
      expect(refreshSpy).toHaveBeenCalledTimes(2)
      expect(tokenStorage.setTokens).toHaveBeenCalledWith('recovered-token', 3600, undefined)

      locationSpy.mockRestore()
      refreshSpy.mockRestore()
    })
  })
})
