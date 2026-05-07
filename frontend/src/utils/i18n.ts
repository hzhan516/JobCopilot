import i18n from '@/i18n';

const TIMEZONE_STORAGE_KEY = 'user-timezone';

/**
 * Returns the user's preferred time zone, falling back to the browser's detected locale.
 * Silently degrades if localStorage is unavailable (e.g., private mode).
 *
 * 获取用户偏好时区；若未设置则回退到浏览器检测的本地时区。
 * localStorage 不可用时静默降级（如隐私模式）。
 */
export function getUserTimeZone(): string {
  try {
    const stored = localStorage.getItem(TIMEZONE_STORAGE_KEY);
    if (stored) return stored;
  } catch {
    // Silently degrade when localStorage is unavailable
    // localStorage 不可用时静默降级
  }
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

export function setUserTimeZone(timeZone: string): void {
  try {
    localStorage.setItem(TIMEZONE_STORAGE_KEY, timeZone);
  } catch {
    // ignore
  }
}

export function clearUserTimeZone(): void {
  try {
    localStorage.removeItem(TIMEZONE_STORAGE_KEY);
  } catch {
    // ignore
  }
}

export function getLocale(): string {
  if (i18n.language === 'zh-TW') return 'zh-TW';
  if (i18n.language === 'zh-CN') return 'zh-CN';
  return 'en-US';
}

/**
 * Formats a date using the user's locale and preferred time zone.
 * Falls back to browser defaults when no preference is stored.
 *
 * 按用户 locale 与偏好时区格式化日期；未设置时回退到浏览器默认值
 */
export function formatDate(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleDateString(getLocale(), {
    timeZone: getUserTimeZone(),
    ...options,
  });
}

export function formatTime(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleTimeString(getLocale(), {
    timeZone: getUserTimeZone(),
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  });
}

export function formatDateTime(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleString(getLocale(), {
    timeZone: getUserTimeZone(),
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  });
}
