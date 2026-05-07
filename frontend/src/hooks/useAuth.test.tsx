import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import type { ReactNode } from 'react'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types'

vi.mock('@/services/api', () => ({
  authService: {
    login: vi.fn(),
    loginByGoogle: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getCurrentUser: vi.fn(),
  },
}))

import { authService } from '@/services/api'
import { AuthProvider, useAuth } from './useAuth'

const mockLogin = vi.mocked(authService.login)
const mockLoginByGoogle = vi.mocked(authService.loginByGoogle)
const mockRegister = vi.mocked(authService.register)
const mockLogout = vi.mocked(authService.logout)
const mockGetCurrentUser = vi.mocked(authService.getCurrentUser)

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>
}

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGetCurrentUser.mockReturnValue(null)
  })

  it('throws when used outside AuthProvider', () => {
    // Suppress console.error for expected throw
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used within an AuthProvider')
    consoleSpy.mockRestore()
  })

  it('initializes with no user when not logged in', () => {
    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.isLoading).toBe(false)
  })

  it('initializes with user when already logged in', () => {
    const user = { userId: 'u1', email: 'test@example.com' }
    mockGetCurrentUser.mockReturnValue(user)

    const { result } = renderHook(() => useAuth(), { wrapper })
    expect(result.current.user).toEqual(user)
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('handles login successfully', async () => {
    const authResponse: AuthResponse = {
      userId: 'u1',
      email: 'test@example.com',
      accessToken: 'token',
      refreshToken: 'refresh',
      expiresIn: 3600,
    }
    mockLogin.mockResolvedValue(authResponse)

    const { result } = renderHook(() => useAuth(), { wrapper })

    const loginData: LoginRequest = { email: 'test@example.com', password: 'password' }

    await act(async () => {
      await result.current.login(loginData)
    })

    expect(mockLogin).toHaveBeenCalledWith(loginData, false)
    expect(result.current.user).toEqual({ userId: 'u1', email: 'test@example.com' })
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.isLoading).toBe(false)
  })

  it('handles Google login', async () => {
    const authResponse: AuthResponse = {
      userId: 'u2',
      email: 'google@example.com',
      accessToken: 'token',
      refreshToken: 'refresh',
      expiresIn: 3600,
    }
    mockLoginByGoogle.mockResolvedValue(authResponse)

    const { result } = renderHook(() => useAuth(), { wrapper })

    await act(async () => {
      await result.current.loginByGoogle('id-token-string', true)
    })

    expect(mockLoginByGoogle).toHaveBeenCalledWith({ idToken: 'id-token-string' }, true)
    expect(result.current.user).toEqual({ userId: 'u2', email: 'google@example.com' })
  })

  it('handles register', async () => {
    const authResponse: AuthResponse = {
      userId: 'u3',
      email: 'new@example.com',
      accessToken: 'token',
      refreshToken: 'refresh',
      expiresIn: 3600,
    }
    mockRegister.mockResolvedValue(authResponse)

    const { result } = renderHook(() => useAuth(), { wrapper })

    const registerData: RegisterRequest = { email: 'new@example.com', password: 'pass' }

    await act(async () => {
      await result.current.register(registerData)
    })

    expect(mockRegister).toHaveBeenCalledWith(registerData, false)
    expect(result.current.user).toEqual({ userId: 'u3', email: 'new@example.com' })
  })

  it('sets isLoading during async operations', async () => {
    let resolveLogin: (value: AuthResponse) => void
    const loginPromise = new Promise<AuthResponse>((resolve) => {
      resolveLogin = resolve
    })
    mockLogin.mockReturnValue(loginPromise)

    const { result } = renderHook(() => useAuth(), { wrapper })

    act(() => {
      result.current.login({ email: 't@test.com', password: 'pass' })
    })

    expect(result.current.isLoading).toBe(true)

    await act(async () => {
      resolveLogin!({
        userId: 'u1',
        email: 't@test.com',
        accessToken: 'tok',
        refreshToken: 'ref',
        expiresIn: 3600,
      })
      await loginPromise
    })

    expect(result.current.isLoading).toBe(false)
  })

  it('handles logout', () => {
    mockGetCurrentUser.mockReturnValue({ userId: 'u1', email: 'test@example.com' })
    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.isAuthenticated).toBe(true)

    act(() => {
      result.current.logout()
    })

    expect(mockLogout).toHaveBeenCalled()
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
