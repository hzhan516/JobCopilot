import { useState, useEffect } from 'react';

/**
 * 响应式媒体查询 hook。
 * @param query - CSS 媒体查询字符串，如 "(min-width: 1024px)"
 * @returns 当前是否匹配
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    const mql = window.matchMedia(query);
    const onChange = (e: MediaQueryListEvent) => setMatches(e.matches);
    mql.addEventListener('change', onChange);
    return () => mql.removeEventListener('change', onChange);
  }, [query]);

  return matches;
}

/** Master-Detail 分栏断点：>=1024px 启用分栏 */
export const MASTER_DETAIL_BREAKPOINT = '(min-width: 1024px)';

/**
 * 便捷 hook：是否应展示 Master-Detail 分栏。
 */
export function useMasterDetail(): boolean {
  return useMediaQuery(MASTER_DETAIL_BREAKPOINT);
}
