import type { Location } from 'react-router-dom';

export const AUTH_DEFAULT_ROUTE = '/';

const AUTH_ROUTES = new Set(['/login', '/register']);

type RedirectState = {
  from?: Partial<Pick<Location, 'pathname' | 'search' | 'hash'>>;
};

function isSafeInternalPath(pathname: string): boolean {
  return pathname.startsWith('/') && !pathname.startsWith('//');
}

export function getPostAuthRedirectPath(state: unknown): string {
  const from = (state as RedirectState | null | undefined)?.from;
  const pathname = from?.pathname;

  if (!pathname || !isSafeInternalPath(pathname) || AUTH_ROUTES.has(pathname)) {
    return AUTH_DEFAULT_ROUTE;
  }

  return `${pathname}${from.search ?? ''}${from.hash ?? ''}`;
}
