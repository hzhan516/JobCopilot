import { describe, expect, it } from 'vitest';
import { AUTH_DEFAULT_ROUTE, getPostAuthRedirectPath } from './authRedirect';

describe('authRedirect', () => {
  it('returns default route when state is empty', () => {
    expect(getPostAuthRedirectPath(undefined)).toBe(AUTH_DEFAULT_ROUTE);
    expect(getPostAuthRedirectPath(null)).toBe(AUTH_DEFAULT_ROUTE);
    expect(getPostAuthRedirectPath({})).toBe(AUTH_DEFAULT_ROUTE);
  });

  it('returns a safe internal from path', () => {
    expect(getPostAuthRedirectPath({ from: { pathname: '/jobs' } })).toBe('/jobs');
  });

  it('preserves search and hash from the original location', () => {
    expect(
      getPostAuthRedirectPath({
        from: {
          pathname: '/applications',
          search: '?edit=tracking-1',
          hash: '#timeline',
        },
      })
    ).toBe('/applications?edit=tracking-1#timeline');
  });

  it('falls back to default route for auth pages', () => {
    expect(getPostAuthRedirectPath({ from: { pathname: '/login' } })).toBe(AUTH_DEFAULT_ROUTE);
    expect(getPostAuthRedirectPath({ from: { pathname: '/register' } })).toBe(AUTH_DEFAULT_ROUTE);
  });

  it('falls back to default route for unsafe external-like paths', () => {
    expect(getPostAuthRedirectPath({ from: { pathname: '//evil.com' } })).toBe(AUTH_DEFAULT_ROUTE);
    expect(getPostAuthRedirectPath({ from: { pathname: 'https://evil.com' } })).toBe(AUTH_DEFAULT_ROUTE);
  });
});
