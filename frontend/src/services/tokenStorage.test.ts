import { describe, it, expect, beforeEach } from 'vitest'
import { tokenStorage } from './tokenStorage'

describe('tokenStorage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  describe('rememberMe', () => {
    it('sets and gets rememberMe', () => {
      tokenStorage.setRememberMe(true)
      expect(tokenStorage.getRememberMe()).toBe(true)

      tokenStorage.setRememberMe(false)
      expect(tokenStorage.getRememberMe()).toBe(false)
    })

    it('defaults to true when not set', () => {
      expect(tokenStorage.getRememberMe()).toBe(true)
    })
  })

  describe('setTokens / getAccessToken', () => {
    it('stores access token in localStorage when rememberMe=true', () => {
      tokenStorage.setTokens('access-123', 3600, true)
      expect(tokenStorage.getAccessToken()).toBe('access-123')
      expect(localStorage.getItem('accessToken')).toBe('access-123')
      expect(sessionStorage.getItem('accessToken')).toBeNull()
    })

    it('stores access token in sessionStorage when rememberMe=false', () => {
      tokenStorage.setTokens('access-789', 3600, false)
      expect(tokenStorage.getAccessToken()).toBe('access-789')
      expect(sessionStorage.getItem('accessToken')).toBe('access-789')
      expect(localStorage.getItem('accessToken')).toBeNull()
    })

    it('sessionStorage takes priority over localStorage for access token', () => {
      tokenStorage.setTokens('local-token', 3600, true)
      tokenStorage.setTokens('session-token', 3600, false)
      expect(tokenStorage.getAccessToken()).toBe('session-token')
    })
  })

  describe('getExpiresAt', () => {
    it('returns null when no expiresAt is set', () => {
      expect(tokenStorage.getExpiresAt()).toBeNull()
    })

    it('returns the correct expiresAt timestamp', () => {
      const before = Date.now()
      tokenStorage.setTokens('a', 3600, true)
      const after = Date.now()
      const expiresAt = tokenStorage.getExpiresAt()
      expect(expiresAt).not.toBeNull()
      expect(expiresAt! >= before + 3600 * 1000).toBe(true)
      expect(expiresAt! <= after + 3600 * 1000).toBe(true)
    })
  })

  describe('setUser / getUser', () => {
    it('stores and retrieves user', () => {
      const user = { userId: 'u1', email: 'test@example.com' }
      tokenStorage.setUser(user, true)
      expect(tokenStorage.getUser()).toEqual(user)
    })

    it('returns null when no user is set', () => {
      expect(tokenStorage.getUser()).toBeNull()
    })

    it('returns null for invalid JSON', () => {
      localStorage.setItem('user', 'not-json')
      expect(tokenStorage.getUser()).toBeNull()
    })
  })

  describe('isTokenExpired', () => {
    it('returns true when no expiresAt is set', () => {
      expect(tokenStorage.isTokenExpired()).toBe(true)
    })

    it('returns true when token is expired', () => {
      tokenStorage.setTokens('a', -3600, true)
      expect(tokenStorage.isTokenExpired()).toBe(true)
    })

    it('returns false for a fresh token', () => {
      tokenStorage.setTokens('a', 3600, true)
      expect(tokenStorage.isTokenExpired()).toBe(false)
    })

    it('considers token expired 5 minutes before actual expiry', () => {
      tokenStorage.setTokens('a', 240, true) // 4 minutes
      expect(tokenStorage.isTokenExpired()).toBe(true)
    })
  })

  describe('clear', () => {
    it('removes all tokens from both storages', () => {
      tokenStorage.setTokens('a', 3600, true)
      tokenStorage.setTokens('a2', 3600, false)
      tokenStorage.setUser({ userId: 'u1', email: 'e' }, true)
      tokenStorage.clear()

      expect(tokenStorage.getAccessToken()).toBeNull()
      expect(tokenStorage.getUser()).toBeNull()
      expect(tokenStorage.getExpiresAt()).toBeNull()
      expect(tokenStorage.getRememberMe()).toBe(true) // rememberMe key is also cleared
    })
  })
})
