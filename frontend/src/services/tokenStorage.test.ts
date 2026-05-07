import { describe, expect, it, vi } from 'vitest';
import tokenStorage from './tokenStorage';

describe('tokenStorage', () => {
  it('stores remembered tokens in localStorage', () => {
    vi.setSystemTime(new Date('2026-05-07T00:00:00Z'));

    tokenStorage.setTokens('access-token', 'refresh-token', 3600, true);
    tokenStorage.setUser({ userId: 'user-1', email: 'user@example.com' }, true);

    expect(localStorage.getItem('accessToken')).toBe('access-token');
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(tokenStorage.getAccessToken()).toBe('access-token');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
    expect(tokenStorage.getRememberMe()).toBe(true);
    expect(tokenStorage.getUser()).toEqual({ userId: 'user-1', email: 'user@example.com' });
    expect(tokenStorage.isTokenExpired()).toBe(false);
  });

  it('stores non-remembered tokens in sessionStorage first', () => {
    localStorage.setItem('accessToken', 'old-local-token');

    tokenStorage.setTokens('session-access-token', 'session-refresh-token', 3600, false);

    expect(sessionStorage.getItem('accessToken')).toBe('session-access-token');
    expect(localStorage.getItem('rememberMe')).toBe('false');
    expect(tokenStorage.getAccessToken()).toBe('session-access-token');
    expect(tokenStorage.getRefreshToken()).toBe('session-refresh-token');
    expect(tokenStorage.getRememberMe()).toBe(false);
  });

  it('clears auth data from both browser storages', () => {
    tokenStorage.setTokens('access-token', 'refresh-token', 3600, true);
    tokenStorage.setUser({ userId: 'user-1', email: 'user@example.com' }, true);
    sessionStorage.setItem('accessToken', 'session-token');

    tokenStorage.clear();

    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(tokenStorage.getUser()).toBeNull();
    expect(localStorage.getItem('rememberMe')).toBeNull();
  });
});
